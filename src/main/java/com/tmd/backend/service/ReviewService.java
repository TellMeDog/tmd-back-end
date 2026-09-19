package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.common.MarkerColor;
import com.tmd.backend.domain.image.ImageUsage;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.review.FeedbackType;
import com.tmd.backend.domain.review.MismatchReason;
import com.tmd.backend.domain.review.Review;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.request.review.ReviewCreateRequest;
import com.tmd.backend.dto.request.review.ReviewUpdateRequest;
import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.place.PlaceDetailResponse;
import com.tmd.backend.dto.response.review.MyReviewListResponse;
import com.tmd.backend.dto.response.review.ReviewDetailResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.PlaceRepository;
import com.tmd.backend.repository.ReviewRepository;
import com.tmd.backend.repository.ReviewVisitStatsProjection;
import com.tmd.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final PetRepository petRepository;
    private final ReviewRepository reviewRepository;
    private final CacheManager cacheManager;
    private final ImageService imageService;
    private final MarkerColorService markerColorService;

    @Transactional
    public void createReview(String email, Long placeId, ReviewCreateRequest request) {
        validateMismatchReasons(request.getFeedbackType(), request.getMismatchReasons());

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BaseException(ErrorCode.UNAUTHORIZED));
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new BaseException(ErrorCode.PLACE_NOT_FOUND));
        Pet pet = request.getPetId() != null
            ? petRepository.findByIdAndUserEmail(request.getPetId(), email)
                .orElseThrow(() -> new BaseException(ErrorCode.FORBIDDEN))
            : null;

        String imageKey = imageService.attachReadyUpload(
            email, request.getImageUploadId(), ImageUsage.REVIEW);

        reviewRepository.save(Review.create(
            user, place, pet,
            request.getFeedbackType(),
            request.getMismatchReasons(),
            request.getEtcReasons(),
            request.getRating(),
            request.getContent(),
            imageKey
        ));
        evictPlaceCache(placeId);
    }

    @Transactional
    public void updateReview(String email, Long reviewId, ReviewUpdateRequest request) {
        validateMismatchReasons(request.getFeedbackType(), request.getMismatchReasons());

        Review review = reviewRepository.findByIdAndUserEmail(reviewId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.REVIEW_NOT_FOUND));

        Long placeId = review.getPlace().getId();

        if (request.isRemoveImage() && request.getImageUploadId() != null) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_UPLOAD);
        }

        String previousImageKey = review.getImageKey();
        String imageKey = previousImageKey;

        if (request.getImageUploadId() != null) {
            imageKey = imageService.attachReadyUpload(
                email, request.getImageUploadId(), ImageUsage.REVIEW);
            imageService.markForDeletion(previousImageKey);
        } else if (request.isRemoveImage()) {
            imageKey = null;
            imageService.markForDeletion(previousImageKey);
        }

        review.update(
            request.getFeedbackType(),
            request.getMismatchReasons(),
            request.getEtcReason(),
            request.getRating(),
            request.getContent(),
            imageKey
        );
        evictPlaceCache(placeId);
    }

    @Transactional
    public void deleteReview(String email, Long reviewId) {
        Review review = reviewRepository.findByIdAndUserEmail(reviewId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.REVIEW_NOT_FOUND));
        deleteReviews(List.of(review));
    }

    @Transactional
    public void deleteReviewsByPetId(Long petId) {
        deleteReviews(reviewRepository.findAllByPetId(petId));
    }

    @Transactional
    public void deleteReviewsByUserId(Long userId) {
        deleteReviews(reviewRepository.findAllByUserId(userId));
    }

    // 장소의 전체 리뷰(페이지네이션)
    public PageResponse<ReviewDetailResponse> getPlaceReviewList(
        Long placeId,
        String email,
        int page,
        int size,
        String sort
    ) {
        PageRequest pageable = createReviewPageRequest(page, size, sort);
        Page<Review> reviewPage = StringUtils.hasText(email)
            ? reviewRepository.findByPlaceIdExcludingUser(placeId, email, pageable)
            : reviewRepository.findByPlaceId(placeId, pageable);

        return new PageResponse<>(
            reviewPage.getContent().stream().map(this::toReviewDetailResponse).toList(),
            reviewPage.getNumber(),
            reviewPage.getSize(),
            reviewPage.getTotalElements(),
            reviewPage.getTotalPages(),
            reviewPage.hasNext()
        );
    }

    public PageResponse<MyReviewListResponse> getMyReviewListInMyPage(Long petId, String email, int page, int size) {
        validatePageRequest(page, size);
        Pet pet = petRepository.findByIdAndUserEmail(petId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.NOT_OWNER_OF_DOG));

        Page<Review> reviewPage = reviewRepository.findByUserEmail(email,
            PageRequest.of(page, size, Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            ))); // 최신순

        return new PageResponse<>(
            reviewPage.getContent().stream().map(review -> toMyReviewListResponse(review, pet)).toList(),
            reviewPage.getNumber(),
            reviewPage.getSize(),
            reviewPage.getTotalElements(),
            reviewPage.getTotalPages(),
            reviewPage.hasNext()
        );
    }

    public List<ReviewDetailResponse> getMyReviewListInPlace(Long placeId, String email) {
        return reviewRepository
            .findByPlaceIdAndUserEmailOrderByCreatedAtDescIdDesc(placeId, email)
            .stream()
            .map(this::toReviewDetailResponse)
            .toList();
    }

    @Cacheable(value = "averageRating", key = "#placeId")
    public double getAverageRating(Long placeId) {
        return reviewRepository.findAverageRatingByPlaceId(placeId).orElse(0.0);
    }

    public Map<Long, Double> getAverageRatings(List<Long> placeIds) {
        if (placeIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.findAverageRatingsByPlaceIds(placeIds).stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Double) row[1]
            ));
    }

    @Cacheable(value = "visitStats", key = "#placeId")
    public PlaceDetailResponse.VisitStats getVisitStats(Long placeId) {
        ReviewVisitStatsProjection summary = reviewRepository.findVisitStatsSummary(placeId);
        long enteredCount = valueOrZero(summary.getEnteredCount());
        long mismatchedCount = valueOrZero(summary.getMismatchedCount());
        long deniedCount = valueOrZero(summary.getDeniedCount());
        String lastReportedAt = summary.getLastReportedAt() != null
            ? summary.getLastReportedAt().toString()
            : null;

        List<String> topBreeds = reviewRepository
            .findTopBreedsByPlaceId(placeId, PageRequest.of(0, 3))
            .stream()
            .map(row -> ((PetBreed) row[0]).getKoreanName())
            .toList();

        return PlaceDetailResponse.VisitStats.builder()
            .enteredCount(enteredCount)
            .mismatchedCount(mismatchedCount)
            .deniedCount(deniedCount)
            .lastReportedAt(lastReportedAt)
            .topBreeds(topBreeds)
            .build();
    }

    private PageRequest createReviewPageRequest(int page, int size, String sort) {
        validatePageRequest(page, size);

        Sort sortOption = switch (sort) {
            case "latest" -> Sort.by(
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            );
            case "rating" -> Sort.by(
                Sort.Order.desc("rating"),
                Sort.Order.desc("createdAt"),
                Sort.Order.desc("id")
            );
            default -> throw new BaseException(ErrorCode.VALIDATION_ERROR);
        };

        return PageRequest.of(page, size, sortOption);
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BaseException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private long valueOrZero(Long value) {
        return value != null ? value : 0L;
    }

    private ReviewDetailResponse toReviewDetailResponse(Review review) {
        Pet pet = review.getPet();
        return ReviewDetailResponse.builder()
            .reviewId(review.getId())
            .placeId(review.getPlace().getId())
            .placeTitle(review.getPlace().getTitle())
            .petId(pet != null ? pet.getId() : null)
            .petName(pet != null ? pet.getName() : null)
            .feedbackType(review.getFeedbackType())
            .mismatchReasons(List.copyOf(review.getMismatchReasons()))
            .etcReason(review.getEtcReason())
            .rating(review.getRating())
            .content(review.getContent())
            .imageUrl(imageService.toPublicUrl(review.getImageKey()))
            .createdAt(review.getCreatedAt().toString())
            .build();
    }

    private MyReviewListResponse toMyReviewListResponse(Review review, Pet pet) {
        MarkerColor color = markerColorService.calculateMarkerColor(review.getPlace().getPlacePetPolicy(), pet);
        return MyReviewListResponse.builder()
            .reviewId(review.getId())
            .placeId(review.getPlace().getId())
            .placeTitle(review.getPlace().getTitle())
            .placeAddr1(review.getPlace().getAddr1())
            .placeAddr2(review.getPlace().getAddr2())
            .imageKey(review.getPlace().getFirstImage())
            .markerColor(color.name())
            .build();
    }

    //캐시 삭제
    private void evictPlaceCache(Long placeId) {
        Cache avg = cacheManager.getCache("averageRating");
        Cache stats = cacheManager.getCache("visitStats");
        if (avg != null) avg.evict(placeId);
        if (stats != null) stats.evict(placeId);
    }

    private void deleteReviews(List<Review> reviews) {
        if (reviews.isEmpty()) return;

        Set<Long> placeIds = reviews.stream()
            .map(review -> review.getPlace().getId())
            .collect(Collectors.toSet());
        reviews.forEach(review -> imageService.markForDeletion(review.getImageKey()));
        reviewRepository.deleteAll(reviews);
        reviewRepository.flush();
        placeIds.forEach(this::evictPlaceCache);
    }

    private void validateMismatchReasons(FeedbackType feedbackType, List<MismatchReason> reasons) {
        boolean hasReasons = reasons != null && !reasons.isEmpty();
        boolean reasonsAllowed = feedbackType == FeedbackType.MISMATCHED_INFO
            || feedbackType == FeedbackType.DENIED;
        if (hasReasons && !reasonsAllowed) {
            throw new BaseException(ErrorCode.INVALID_REVIEW_REQUEST);
        }
    }
}
