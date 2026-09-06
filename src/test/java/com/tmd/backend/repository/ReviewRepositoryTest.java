package com.tmd.backend.repository;

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

    // ===== findTop5ByPlaceIdOrderByCreatedAtDesc =====

    @Test
    @DisplayName("리뷰 5개 이상이어도 최대 5개만 반환")
    void findTop5ByPlaceIdOrderByCreatedAtDesc_5개제한() {
        for (int i = 0; i < 7; i++) {
            em.persist(review(FeedbackType.ENTERED, 3));
        }
        em.flush();

        List<Review> result = reviewRepository.findTop5ByPlaceIdOrderByCreatedAtDesc(place.getId());

        assertThat(result).hasSize(5);
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

    // 헬퍼
    private Review review(FeedbackType feedbackType, int rating) {
        return Review.create(user, place, null, feedbackType, null, null, rating, null, null);
    }
}
