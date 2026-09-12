package com.tmd.backend.domain.favorite;

import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(
    name = "uk_favorite_user_place",
    columnNames = {"user_id", "place_id"}
))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @Builder
    private Favorite(User user, Place place) {
        this.user = user;
        this.place = place;
    }
    public static Favorite create(User user, Place place) {
        return Favorite.builder()
            .user(user)
            .place(place)
            .build();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
