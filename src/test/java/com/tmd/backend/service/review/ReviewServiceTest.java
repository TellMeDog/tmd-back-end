package com.tmd.backend.service.review;

import com.tmd.backend.service.image.ImageService;
import com.tmd.backend.service.place.MarkerColorService;

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
import com.tmd.backend.dto.response.review.ReviewDetailResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.pet.PetRepository;
import com.tmd.backend.repository.place.PlaceRepository;
import com.tmd.backend.repository.review.ReviewRepository;
import com.tmd.backend.repository.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock UserRepository userRepository;
    @Mock PlaceRepository placeRepository;
    @Mock PetRepository petRepository;
    @Mock ReviewRepository reviewRepository;
    @Mock CacheManager cacheManager;
    @Mock ImageService imageService;
    @Mock MarkerColorService markerColorService;

    @InjectMocks ReviewService reviewService;

    // ===== createReview =====

    @Test
    @DisplayName("petId 없이도 리뷰 생성 성공")
    void createReview_petId없이_성공() {
        ReviewCreateRequest request = mock(ReviewCreateRequest.class);
        given(request.getFeedbackType()).willReturn(FeedbackType.ENTERED);
        given(request.getMismatchReasons()).willReturn(null);
        given(request.getPetId()).willReturn(null);
        given(request.getRating()).willReturn(5);

        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(mock(User.class)));
        given(placeRepository.findById(1L)).willReturn(Optional.of(mock(Place.class)));
        Cache mockCache = mock(Cache.class);
        given(cacheManager.getCache(any())).willReturn(mockCache);

        reviewService.createReview("test@email.com", 1L, request);

        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("petId 있을 때 Pet 조회 후 리뷰 생성")
    void createReview_petId있을때_성공() {
        ReviewCreateRequest request = mock(ReviewCreateRequest.class);
        given(request.getFeedbackType()).willReturn(FeedbackType.ENTERED);
        given(request.getMismatchReasons()).willReturn(null);
        given(request.getPetId()).willReturn(10L);
        given(request.getRating()).willReturn(4);

        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(mock(User.class)));
        given(placeRepository.findById(1L)).willReturn(Optional.of(mock(Place.class)));
        given(petRepository.findByIdAndUserEmail(10L, "test@email.com"))
            .willReturn(Optional.of(mock(Pet.class)));
        Cache mockCache = mock(Cache.class);
        given(cacheManager.getCache(any())).willReturn(mockCache);

        reviewService.createReview("test@email.com", 1L, request);

        verify(petRepository).findByIdAndUserEmail(10L, "test@email.com");
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("ENTERED feedbackType에 mismatchReasons 있으면 예외")
    void createReview_ENTERED에_mismatchReasons_예외() {
        ReviewCreateRequest request = mock(ReviewCreateRequest.class);
        given(request.getFeedbackType()).willReturn(FeedbackType.ENTERED);
        given(request.getMismatchReasons()).willReturn(List.of(MismatchReason.LEASH_REQUIRED));

        assertThatThrownBy(() -> reviewService.createReview("test@email.com", 1L, request))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REVIEW_REQUEST));
    }

    @Test
    @DisplayName("MISMATCHED_INFO에 mismatchReasons 있으면 정상")
    void createReview_MISMATCHED_INFO에_mismatchReasons_성공() {
        ReviewCreateRequest request = mock(ReviewCreateRequest.class);
        given(request.getFeedbackType()).willReturn(FeedbackType.MISMATCHED_INFO);
        given(request.getMismatchReasons()).willReturn(List.of(MismatchReason.LEASH_REQUIRED));
        given(request.getPetId()).willReturn(null);
        given(request.getRating()).willReturn(2);

        given(userRepository.findByEmail("test@email.com")).willReturn(Optional.of(mock(User.class)));
        given(placeRepository.findById(1L)).willReturn(Optional.of(mock(Place.class)));
        Cache mockCache = mock(Cache.class);
        given(cacheManager.getCache(any())).willReturn(mockCache);

        reviewService.createReview("test@email.com", 1L, request);

        verify(reviewRepository).save(any(Review.class));
    }

    // ===== updateReview =====

    @Test
    @DisplayName("본인 리뷰 수정 성공 — 캐시 무효화 호출")
    void updateReview_성공_캐시무효화() {
        Review review = mock(Review.class);
        Place place = mock(Place.class);
        given(place.getId()).willReturn(1L);
        given(review.getPlace()).willReturn(place);
        given(reviewRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(review));

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getFeedbackType()).willReturn(FeedbackType.ENTERED);
        given(request.getMismatchReasons()).willReturn(null);
        given(request.getRating()).willReturn(5);

        Cache mockCache = mock(Cache.class);
        given(cacheManager.getCache(any())).willReturn(mockCache);

        reviewService.updateReview("test@email.com", 1L, request);

        verify(review).update(any(), any(), any(), anyInt(), any(), any());
        verify(mockCache, times(2)).evict(1L); // averageRating + visitStats
    }

    @Test
    @DisplayName("타인 리뷰 수정 시 REVIEW_NOT_FOUND 예외")
    void updateReview_타인리뷰_예외() {
        given(reviewRepository.findByIdAndUserEmail(1L, "other@email.com"))
            .willReturn(Optional.empty());

        ReviewUpdateRequest request = mock(ReviewUpdateRequest.class);
        given(request.getFeedbackType()).willReturn(FeedbackType.ENTERED);
        given(request.getMismatchReasons()).willReturn(null);

        assertThatThrownBy(() -> reviewService.updateReview("other@email.com", 1L, request))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    // ===== deleteReview =====

    @Test
    @DisplayName("리뷰 삭제 성공 — 캐시 무효화 호출")
    void deleteReview_성공_캐시무효화() {
        Review review = mock(Review.class);
        Place place = mock(Place.class);
        given(place.getId()).willReturn(1L);
        given(review.getPlace()).willReturn(place);
        given(review.getImageKey()).willReturn("images/review/delete.jpg");
        given(reviewRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(review));

        Cache mockCache = mock(Cache.class);
        given(cacheManager.getCache(any())).willReturn(mockCache);

        reviewService.deleteReview("test@email.com", 1L);

        verify(imageService).markForDeletion("images/review/delete.jpg");
        verify(reviewRepository).deleteAll(List.of(review));
        verify(reviewRepository).flush();
        verify(mockCache, times(2)).evict(1L);
    }

    @Test
    void deleteReviewsByPetId_deletesImagesAndEvictsEachPlaceOnce() {
        Review firstReview = mock(Review.class);
        Review secondReview = mock(Review.class);
        Place place = mock(Place.class);
        given(place.getId()).willReturn(1L);
        given(firstReview.getPlace()).willReturn(place);
        given(secondReview.getPlace()).willReturn(place);
        given(firstReview.getImageKey()).willReturn("images/review/first.jpg");
        given(secondReview.getImageKey()).willReturn("images/review/second.jpg");
        given(reviewRepository.findAllByPetId(10L))
            .willReturn(List.of(firstReview, secondReview));
        Cache mockCache = mock(Cache.class);
        given(cacheManager.getCache(any())).willReturn(mockCache);

        reviewService.deleteReviewsByPetId(10L);

        verify(imageService).markForDeletion("images/review/first.jpg");
        verify(imageService).markForDeletion("images/review/second.jpg");
        verify(reviewRepository).deleteAll(List.of(firstReview, secondReview));
        verify(reviewRepository).flush();
        verify(mockCache, times(2)).evict(1L);
    }

    @Test
    @DisplayName("타인 리뷰 삭제 시 REVIEW_NOT_FOUND 예외")
    void deleteReview_타인리뷰_예외() {
        given(reviewRepository.findByIdAndUserEmail(1L, "other@email.com"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview("other@email.com", 1L))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.REVIEW_NOT_FOUND));
    }

    // ===== getAverageRating =====

    @Test
    @DisplayName("리뷰 없으면 평균 0.0 반환")
    void getAverageRating_리뷰없으면_0() {
        given(reviewRepository.findAverageRatingByPlaceId(1L)).willReturn(Optional.empty());

        double result = reviewService.getAverageRating(1L);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    @DisplayName("평균 평점 정상 반환")
    void getAverageRating_정상반환() {
        given(reviewRepository.findAverageRatingByPlaceId(1L)).willReturn(Optional.of(4.2));

        double result = reviewService.getAverageRating(1L);

        assertThat(result).isEqualTo(4.2);
    }

    @Test
    void getAverageRatings_장소별평균을한번에반환() {
        given(reviewRepository.findAverageRatingsByPlaceIds(List.of(1L, 2L, 3L)))
            .willReturn(List.of(new Object[]{1L, 4.5}, new Object[]{3L, 2.0}));

        var result = reviewService.getAverageRatings(List.of(1L, 2L, 3L));

        assertThat(result).containsEntry(1L, 4.5).containsEntry(3L, 2.0);
        assertThat(result).doesNotContainKey(2L);
    }

    @Test
    void getAverageRatings_빈목록이면DB를조회하지않음() {
        assertThat(reviewService.getAverageRatings(List.of())).isEmpty();
        verifyNoInteractions(reviewRepository);
    }

    @Test
    @DisplayName("평점순 리뷰 페이지는 안정적인 보조 정렬과 페이지 정보를 사용한다")
    void getPlaceReviewList_평점순정렬과페이지정보() {
        given(reviewRepository.findByPlaceIdExcludingUser(anyLong(), anyString(), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(), PageRequest.of(1, 2), 3));

        PageResponse<ReviewDetailResponse> result = reviewService.getPlaceReviewList(
            1L,
            "test@email.com",
            1,
            2,
            "rating"
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(reviewRepository).findByPlaceIdExcludingUser(
            eq(1L),
            eq("test@email.com"),
            pageableCaptor.capture()
        );

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort().getOrderFor("rating").isDescending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("id").isDescending()).isTrue();
        assertThat(result.getPage()).isEqualTo(1);
        assertThat(result.getSize()).isEqualTo(2);
        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    @DisplayName("비회원 장소 상세에서는 사용자를 제외하지 않고 전체 리뷰를 조회한다")
    void getPlaceReviewList_비회원_전체조회() {
        given(reviewRepository.findByPlaceId(eq(1L), any(Pageable.class)))
            .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        reviewService.getPlaceReviewList(1L, null, 0, 10, "latest");

        verify(reviewRepository).findByPlaceId(eq(1L), any(Pageable.class));
        verify(reviewRepository, never())
            .findByPlaceIdExcludingUser(anyLong(), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("리뷰 페이지 크기는 1부터 100까지만 허용한다")
    void getPlaceReviewList_페이지크기검증() {
        assertThatThrownBy(() -> reviewService.getPlaceReviewList(
            1L,
            "test@email.com",
            0,
            101,
            "latest"
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verifyNoInteractions(reviewRepository);
    }

    @Test
    void getMyReviewListInMyPage_페이지크기검증() {
        assertThatThrownBy(() -> reviewService.getMyReviewListInMyPage(
            1L,
            "test@email.com",
            -1,
            10
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verifyNoInteractions(petRepository, reviewRepository);
    }
}
