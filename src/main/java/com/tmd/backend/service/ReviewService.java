package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
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

import java.util.List;
import java.util.Map;
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

        reviewRepository.save(Review.create(
            user, place, pet,
            request.getFeedbackType(),
            request.getMismatchReasons(),
            request.getEtcReasons(),
            request.getRating(),
            request.getContent(),
            request.getImageKey()
        ));
        evictPlaceCache(placeId);
    }

    @Transactional
    public void updateReview(String email, Long reviewId, ReviewUpdateRequest request) {
        validateMismatchReasons(request.getFeedbackType(), request.getMismatchReasons());

        Review review = reviewRepository.findByIdAndUserEmail(reviewId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.REVIEW_NOT_FOUND));
        Long placeId = review.getPlace().getId();

        review.update(
            request.getFeedbackType(),
            request.getMismatchReasons(),
            request.getEtcReason(),
            request.getRating(),
            request.getContent(),
            request.getImageKey()
        );
        evictPlaceCache(placeId);
    }

    @Transactional
    public void deleteReview(String email, Long reviewId) {
        Review review = reviewRepository.findByIdAndUserEmail(reviewId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.REVIEW_NOT_FOUND));
        Long placeId = review.getPlace().getId();
        reviewRepository.delete(review);
        evictPlaceCache(placeId);
    }

    // 장소의 전체 리뷰(페이지네이션)
    public PageResponse<ReviewDetailResponse> getPlaceReviewList(
        Long placeId,
        String email,
        int page,
        int size,
        String sort
    ) {
        Page<Review> reviewPage = reviewRepository.findByPlaceIdExcludingUser(
            placeId,
            email,
            createReviewPageRequest(page, size, sort)
        );

        return new PageResponse<>(
            reviewPage.getContent().stream().map(this::toReviewDetailResponse).toList(),
            reviewPage.getNumber(),
            reviewPage.getSize(),
            reviewPage.getTotalElements(),
            reviewPage.getTotalPages(),
            reviewPage.hasNext()
        );
    }

    public PageResponse<MyReviewListResponse> getMyReviewListInMyPage(String email, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByUserEmail(email,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))); // 최신순

        return new PageResponse<>(
            reviewPage.getContent().stream().map(this::toMyReviewListResponse).toList(),
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

    @Cacheable(value = "visitStats", key = "#placeId")
    public PlaceDetailResponse.VisitStats getVisitStats(Long placeId) {
        Map<FeedbackType, Long> counts = reviewRepository.countGroupByFeedbackType(placeId)
            .stream()
            .collect(Collectors.toMap(row -> (FeedbackType) row[0], row -> (Long) row[1]));

        String lastReportedAt = reviewRepository.findLastReportedAtByPlaceId(placeId)
            .map(Object::toString)
            .orElse(null);

        List<String> topBreeds = reviewRepository
            .findTopBreedsByPlaceId(placeId, PageRequest.of(0, 3))
            .stream()
            .map(row -> ((PetBreed) row[0]).name())
            .toList();

        return PlaceDetailResponse.VisitStats.builder()
            .enteredCount(counts.getOrDefault(FeedbackType.ENTERED, 0L))
            .mismatchedCount(counts.getOrDefault(FeedbackType.MISMATCHED_INFO, 0L))
            .deniedCount(counts.getOrDefault(FeedbackType.DENIED, 0L))
            .lastReportedAt(lastReportedAt)
            .topBreeds(topBreeds)
            .build();
    }

    private PageRequest createReviewPageRequest(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BaseException(ErrorCode.VALIDATION_ERROR);
        }

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

    private ReviewDetailResponse toReviewDetailResponse(Review review) {
        Pet pet = review.getPet();
        return ReviewDetailResponse.builder()
            .reviewId(review.getId())
            .placeId(review.getPlace().getId())
            .placeTitle(review.getPlace().getTitle())
            .petId(pet != null ? pet.getId() : null)
            .petName(pet != null ? pet.getName() : null)
            .feedbackType(review.getFeedbackType())
            .mismatchReasons(review.getMismatchReasons())
            .etcReason(review.getEtcReason())
            .rating(review.getRating())
            .content(review.getContent())
            .imageKey(review.getImageKey())
            .createdAt(review.getCreatedAt().toString())
            .build();
    }

    private MyReviewListResponse toMyReviewListResponse(Review review) {
        return MyReviewListResponse.builder()
            .reviewId(review.getId())
            .placeId(review.getPlace().getId())
            .placeTitle(review.getPlace().getTitle())
            .placeAddr1(review.getPlace().getAddr1())
            .placeAddr2(review.getPlace().getAddr2())
            .imageKey(review.getPlace().getFirstImage())
            .markerColor("GREEN") // TODO: 마커 판정 로직 후 채울 것
            .build();
    }

    //캐시 삭제
    private void evictPlaceCache(Long placeId) {
        Cache avg = cacheManager.getCache("averageRating");
        Cache stats = cacheManager.getCache("visitStats");
        if (avg != null) avg.evict(placeId);
        if (stats != null) stats.evict(placeId);
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
