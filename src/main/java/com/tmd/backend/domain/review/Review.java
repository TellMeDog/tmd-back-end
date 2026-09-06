package com.tmd.backend.domain.review;

import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @Enumerated(EnumType.STRING)
    private FeedbackType feedbackType;

    @ElementCollection
    @CollectionTable(name = "review_mismatch_reason", joinColumns = @JoinColumn(name = "review_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "reason")
    private List<MismatchReason> mismatchReasons = new ArrayList<>();

    private String etcReason;

    @Column(nullable = false)
    private int rating;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String imageKey;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate(){
        this.createdAt=LocalDateTime.now();
    }

    @Builder
    private Review(User user, Place place, Pet pet, FeedbackType feedbackType,
                   List<MismatchReason> mismatchReasons, String etcReason,
                   int rating, String content, String imageKey) {
        this.user = user;
        this.place = place;
        this.pet = pet;
        this.feedbackType = feedbackType;
        this.mismatchReasons = mismatchReasons != null ? mismatchReasons : new ArrayList<>();
        this.etcReason = etcReason;
        this.rating = rating;
        this.content = content;
        this.imageKey = imageKey;
    }

    public void update(FeedbackType feedbackType, List<MismatchReason> mismatchReasons,
                       String etcReason, int rating, String content, String imageKey) {
        this.feedbackType = feedbackType;
        this.mismatchReasons = mismatchReasons != null ? mismatchReasons : new ArrayList<>();
        this.etcReason = etcReason;
        this.rating = rating;
        this.content = content;
        this.imageKey = imageKey;
    }

    public static Review create(User user, Place place, Pet pet, FeedbackType feedbackType,
                                List<MismatchReason> mismatchReasons, String etcReason,
                                int rating, String content, String imageKey) {
        return Review.builder()
            .user(user)
            .place(place)
            .pet(pet)
            .feedbackType(feedbackType)
            .mismatchReasons(mismatchReasons)
            .etcReason(etcReason)
            .rating(rating)
            .content(content)
            .imageKey(imageKey)
            .build();
    }
}
