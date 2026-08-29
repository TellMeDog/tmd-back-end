package com.tmd.backend.domain.place;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FetchedRegion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private double gridLat;
    private double gridLng;
    private LocalDateTime lastFetchedAt;

    @Builder
    public FetchedRegion(double gridLat, double gridLng) {
        this.gridLat = gridLat;
        this.gridLng = gridLng;
    }

    public boolean isStale(Duration ttl) {
        return this.lastFetchedAt.isBefore(LocalDateTime.now().minus(ttl));
    }
}
