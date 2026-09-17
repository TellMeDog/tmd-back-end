package com.tmd.backend.domain.place;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    uniqueConstraints = @UniqueConstraint(
        name = "uk_policy_review_place_source_version",
        columnNames = {"place_id", "source_modified_time"}
    )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlacePetPolicyReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "source_modified_time", nullable = false)
    private String sourceModifiedTime;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String reviewReasons;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String suggestedPolicyJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PolicyReviewStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime resolvedAt;

    public static PlacePetPolicyReview pending(
        Place place,
        String sourceModifiedTime,
        String reviewReasons,
        String suggestedPolicyJson
    ) {
        PlacePetPolicyReview review = new PlacePetPolicyReview();
        review.place = place;
        review.sourceModifiedTime = sourceModifiedTime;
        review.reviewReasons = reviewReasons;
        review.suggestedPolicyJson = suggestedPolicyJson;
        review.status = PolicyReviewStatus.PENDING;
        review.createdAt = LocalDateTime.now();
        return review;
    }
}
