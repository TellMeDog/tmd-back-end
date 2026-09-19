package com.tmd.backend.repository.review;

import com.tmd.backend.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @EntityGraph(attributePaths = "place")
    Optional<Review> findByIdAndUserEmail(Long reviewId, String email);

    @EntityGraph(attributePaths = "place")
    List<Review> findAllByPetId(Long petId);

    @EntityGraph(attributePaths = "place")
    List<Review> findAllByUserId(Long userId);

    @Query(
        value = """
            SELECT r
            FROM Review r
            JOIN FETCH r.place p
            LEFT JOIN FETCH p.placePetPolicy
            WHERE r.user.email = :email
            """,
        countQuery = "SELECT COUNT(r) FROM Review r WHERE r.user.email = :email"
    )
    Page<Review> findByUserEmail(String email, Pageable pageable);

    @EntityGraph(attributePaths = {"place", "pet"})
    List<Review> findByPlaceIdAndUserEmailOrderByCreatedAtDescIdDesc(Long placeId, String email);

    @Query(
        value = """
            SELECT r
            FROM Review r
            JOIN r.user u
            JOIN FETCH r.place
            LEFT JOIN FETCH r.pet
            WHERE r.place.id = :placeId
              AND u.email <> :email
            """,
        countQuery = """
            SELECT COUNT(r)
            FROM Review r
            WHERE r.place.id = :placeId
              AND r.user.email <> :email
            """
    )
    Page<Review> findByPlaceIdExcludingUser(
        @Param("placeId") Long placeId,
        @Param("email") String email,
        Pageable pageable
    );

    @EntityGraph(attributePaths = {"place", "pet"})
    Page<Review> findByPlaceId(Long placeId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.place.id = :placeId")
    Optional<Double> findAverageRatingByPlaceId(@Param("placeId") Long placeId);

    @Query("""
        SELECT r.place.id, AVG(r.rating)
        FROM Review r
        WHERE r.place.id IN :placeIds
        GROUP BY r.place.id
        """)
    List<Object[]> findAverageRatingsByPlaceIds(@Param("placeIds") List<Long> placeIds);

    @Query("""
        SELECT
            SUM(CASE WHEN r.feedbackType = com.tmd.backend.domain.review.FeedbackType.ENTERED THEN 1L ELSE 0L END) AS enteredCount,
            SUM(CASE WHEN r.feedbackType = com.tmd.backend.domain.review.FeedbackType.MISMATCHED_INFO THEN 1L ELSE 0L END) AS mismatchedCount,
            SUM(CASE WHEN r.feedbackType = com.tmd.backend.domain.review.FeedbackType.DENIED THEN 1L ELSE 0L END) AS deniedCount,
            MAX(r.createdAt) AS lastReportedAt
        FROM Review r
        WHERE r.place.id = :placeId
        """)
    ReviewVisitStatsProjection findVisitStatsSummary(@Param("placeId") Long placeId);

    @Query("""
        SELECT r.pet.breed, COUNT(r)
        FROM Review r
        WHERE r.place.id = :placeId
          AND r.pet IS NOT NULL
        GROUP BY r.pet.breed
        ORDER BY COUNT(r) DESC, r.pet.breed ASC
        """)
    List<Object[]> findTopBreedsByPlaceId(@Param("placeId") Long placeId, Pageable pageable);
}
