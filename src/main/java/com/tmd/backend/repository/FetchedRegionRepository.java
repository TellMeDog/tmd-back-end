package com.tmd.backend.repository;

import com.tmd.backend.domain.place.FetchedRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FetchedRegionRepository extends JpaRepository<FetchedRegion, Long> {
    Optional<FetchedRegion> findByGridLatAndGridLng(double gridLat, double gridLng);
}
