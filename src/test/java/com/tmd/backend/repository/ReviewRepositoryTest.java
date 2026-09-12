package com.tmd.backend.repository;

import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.review.FeedbackType;
import com.tmd.backend.domain.review.Review;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.config.QuerydslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
class ReviewRepositoryTest {

    @Autowired TestEntityManager em;
    @Autowired ReviewRepository reviewRepository;

    private User user;
    private Place place;

    @BeforeEach
    void setUp() {
        user = em.persist(User.createLocal("test@email.com", "encoded-password"));
        place = em.persist(Place.create(
            "CONTENT001", null, "서울시 강남구", null, "테스트 장소",
            127.05, 37.50, null, null, null,
            null, null, "FD", "FD05", null
        ));
        em.flush();
    }

    // ===== findByIdAndUserEmail =====

    @Test
    @DisplayName("본인 리뷰 조회 성공")
    void findByIdAndUserEmail_성공() {
        Review review = em.persist(review(FeedbackType.ENTERED, 5));
        em.flush();

        Optional<Review> result = reviewRepository.findByIdAndUserEmail(review.getId(), "test@email.com");

        assertThat(result).isPresent();
        assertThat(result.get().getRating()).isEqualTo(5);
    }

    @Test
    @DisplayName("다른 사용자 이메일로 조회 시 empty")
    void findByIdAndUserEmail_타인이메일_empty() {
        Review review = em.persist(review(FeedbackType.ENTERED, 5));
        em.flush();

        Optional<Review> result = reviewRepository.findByIdAndUserEmail(review.getId(), "other@email.com");

        assertThat(result).isEmpty();
    }

    // ===== findAverageRatingByPlaceId =====

    @Test
    @DisplayName("리뷰 없으면 empty 반환")
    void findAverageRatingByPlaceId_리뷰없으면_empty() {
        Optional<Double> result = reviewRepository.findAverageRatingByPlaceId(place.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("평균 평점 정상 집계")
    void findAverageRatingByPlaceId_정상집계() {
        em.persist(review(FeedbackType.ENTERED, 4));
        em.persist(review(FeedbackType.ENTERED, 2));
        em.flush();

        Optional<Double> result = reviewRepository.findAverageRatingByPlaceId(place.getId());

        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(3.0);
    }

    // ===== countGroupByFeedbackType =====

    @Test
    @DisplayName("feedbackType별 카운트 집계")
    void countGroupByFeedbackType_정상집계() {
        em.persist(review(FeedbackType.ENTERED, 5));
        em.persist(review(FeedbackType.ENTERED, 4));
        em.persist(review(FeedbackType.DENIED, 1));
        em.flush();

        List<Object[]> rows = reviewRepository.countGroupByFeedbackType(place.getId());
        Map<FeedbackType, Long> counts = rows.stream()
            .collect(Collectors.toMap(r -> (FeedbackType) r[0], r -> (Long) r[1]));

        assertThat(counts.get(FeedbackType.ENTERED)).isEqualTo(2L);
        assertThat(counts.get(FeedbackType.DENIED)).isEqualTo(1L);
        assertThat(counts.containsKey(FeedbackType.MISMATCHED_INFO)).isFalse();
    }

    // ===== findLastReportedAtByPlaceId =====

    @Test
    @DisplayName("가장 최근 리뷰 작성 시각 조회")
    void findLastReportedAtByPlaceId_최근시각조회() {
        Review review = em.persist(review(FeedbackType.ENTERED, 3));
        em.flush();

        var result = reviewRepository.findLastReportedAtByPlaceId(place.getId());

        assertThat(result).isPresent();
        assertThat(result.orElseThrow()).isEqualToIgnoringNanos(review.getCreatedAt());
    }

    @Test
    @DisplayName("리뷰가 없으면 최근 작성 시각은 empty")
    void findLastReportedAtByPlaceId_리뷰없으면_empty() {
        assertThat(reviewRepository.findLastReportedAtByPlaceId(place.getId())).isEmpty();
    }

    @Test
    @DisplayName("방문 견종을 리뷰 수가 많은 순서로 제한 조회")
    void findTopBreedsByPlaceId_상위견종조회() {
        Pet mock1 = em.persist(Pet.create(
            user, "첫째", PetBreed.SHIBA_INU, 8.5, null, false, true, false));
        Pet mock2 = em.persist(Pet.create(
            user, "둘째", PetBreed.BEAGLE, 12.0, null, false, true, true));
        em.persist(Review.create(user, place, mock1, FeedbackType.ENTERED, null, null, 5, null, null));
        em.persist(Review.create(user, place, mock1, FeedbackType.ENTERED, null, null, 4, null, null));
        em.persist(Review.create(user, place, mock2, FeedbackType.ENTERED, null, null, 3, null, null));
        em.flush();

        List<Object[]> result = reviewRepository.findTopBreedsByPlaceId(
            place.getId(),
            PageRequest.of(0, 1)
        );

        assertThat(result).hasSize(1);
        assertThat(result.getFirst()[0]).isEqualTo(PetBreed.SHIBA_INU);
        assertThat(result.getFirst()[1]).isEqualTo(2L);
    }

    // ===== findByUserEmail (pagination) =====

    @Test
    @DisplayName("내 리뷰 목록 페이지네이션")
    void findByUserEmail_페이지네이션() {
        for (int i = 0; i < 3; i++) {
            em.persist(review(FeedbackType.ENTERED, 4));
        }
        em.flush();

        Page<Review> page = reviewRepository.findByUserEmail(
            "test@email.com", PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("장소 상세의 내 리뷰는 다른 장소 리뷰를 제외한다")
    void findByPlaceIdAndUserEmail_장소필터링() {
        Place otherPlace = em.persist(Place.create(
            "CONTENT002", null, "서울시 서초구", null, "다른 장소",
            127.01, 37.49, null, null, null,
            null, null, "FD", "FD05", null
        ));
        Review expected = em.persist(review(FeedbackType.ENTERED, 5));
        em.persist(review(user, otherPlace, FeedbackType.DENIED, 1));
        em.flush();

        List<Review> result = reviewRepository
            .findByPlaceIdAndUserEmailOrderByCreatedAtDescIdDesc(place.getId(), user.getEmail());

        assertThat(result).containsExactly(expected);
    }

    @Test
    @DisplayName("전체 리뷰 페이지는 현재 사용자의 리뷰를 제외한다")
    void findByPlaceIdExcludingUser_내리뷰제외() {
        User otherUser = em.persist(User.createLocal("other@email.com", "encoded-password"));
        em.persist(review(FeedbackType.ENTERED, 5));
        Review otherReview = em.persist(review(otherUser, place, FeedbackType.ENTERED, 4));
        em.flush();

        Page<Review> result = reviewRepository.findByPlaceIdExcludingUser(
            place.getId(),
            user.getEmail(),
            PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"))
        );

        assertThat(result.getContent()).containsExactly(otherReview);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    // 헬퍼
    private Review review(FeedbackType feedbackType, int rating) {
        return review(user, place, feedbackType, rating);
    }

    private Review review(User reviewUser, Place reviewPlace, FeedbackType feedbackType, int rating) {
        return Review.create(
            reviewUser,
            reviewPlace,
            null,
            feedbackType,
            null,
            null,
            rating,
            null,
            null
        );
    }
}
