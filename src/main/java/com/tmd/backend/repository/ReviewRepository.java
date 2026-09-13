package com.tmd.backend.repository;

import com.tmd.backend.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Optional<Review> findByIdAndUserEmail(Long reviewId, String email);

    Page<Review> findByUserEmail(String email, Pageable pageable);

    List<Review> findByPlaceIdAndUserEmailOrderByCreatedAtDescIdDesc(Long placeId, String email);

    @Query(
        value = """
            SELECT r
            FROM Review r
            JOIN FETCH r.user
            LEFT JOIN FETCH r.pet
            WHERE r.place.id = :placeId
              AND r.user.email <> :email
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

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.place.id = :placeId")
    Optional<Double> findAverageRatingByPlaceId(@Param("placeId") Long placeId);

    @Query("SELECT r.feedbackType, COUNT(r) FROM Review r WHERE r.place.id = :placeId GROUP BY r.feedbackType")
    List<Object[]> countGroupByFeedbackType(@Param("placeId") Long placeId);

    @Query("SELECT MAX(r.createdAt) FROM Review r WHERE r.place.id = :placeId")
    Optional<LocalDateTime> findLastReportedAtByPlaceId(@Param("placeId") Long placeId);

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
