package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.pet.Pet;
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
import com.tmd.backend.dto.response.review.PlaceReviewItemResponse;
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
            ? petRepository.findById(request.getPetId())
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

    public PageResponse<PlaceReviewItemResponse> getPlaceReviewList(Long placeId, int page, int size, String sort) {
        Sort sortOption = "rating".equals(sort)
            ? Sort.by(Sort.Direction.DESC, "rating")
            : Sort.by(Sort.Direction.DESC, "createdAt");
        Page<Review> reviewPage = reviewRepository.findByPlaceIdWithUser(placeId,
            PageRequest.of(page, size, sortOption));

        return new PageResponse<>(
            reviewPage.getContent().stream().map(this::toPlaceReviewItemResponse).toList(),
            (int) reviewPage.getTotalElements(),
            reviewPage.getTotalPages()
        );
    }

    public ReviewDetailResponse getMyReview(Long reviewId, String email) {
        Review review = reviewRepository.findByIdAndUserEmail(reviewId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.REVIEW_NOT_FOUND));
        return toReviewDetailResponse(review);
    }

    public PageResponse<MyReviewListResponse> getMyReviewList(String email, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByUserEmail(email,
            PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));

        return new PageResponse<>(
            reviewPage.getContent().stream().map(this::toMyReviewListResponse).toList(),
            (int) reviewPage.getTotalElements(),
            reviewPage.getTotalPages()
        );
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

        List<Review> top5 = reviewRepository.findTop5ByPlaceIdOrderByCreatedAtDesc(placeId);
        String lastReportedAt = top5.isEmpty() ? null : top5.get(0).getCreatedAt().toString();

        List<String> topBreeds = reviewRepository.findTopBreedByPlaceId(placeId).stream()
            .map(row -> (String) row[0])
            .limit(3)
            .toList();

        return PlaceDetailResponse.VisitStats.builder()
            .enteredCount(counts.getOrDefault(FeedbackType.ENTERED, 0L))
            .mismatchedCount(counts.getOrDefault(FeedbackType.MISMATCHED_INFO, 0L))
            .deniedCount(counts.getOrDefault(FeedbackType.DENIED, 0L))
            .lastReportedAt(lastReportedAt)
            .topBreeds(topBreeds)
            .build();
    }

    public List<PlaceReviewItemResponse> getRecentReviews(Long placeId) {
        return reviewRepository.findTop5ByPlaceIdOrderByCreatedAtDesc(placeId).stream()
            .map(this::toPlaceReviewItemResponse)
            .toList();
    }

    private PlaceReviewItemResponse toPlaceReviewItemResponse(Review review) {
        return PlaceReviewItemResponse.builder()
            .reviewId(review.getId())
            .writer(review.getUser().getEmail())
            .feedbackType(review.getFeedbackType())
            .mismatchReasons(review.getMismatchReasons())
            .etcReason(review.getEtcReason())
            .rating(review.getRating())
            .content(review.getContent())
            .imageKey(review.getImageKey())
            .createdAt(review.getCreatedAt().toString())
            .build();
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
