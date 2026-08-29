package com.tmd.backend.repository;

import com.tmd.backend.domain.place.PlacePetInfo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlacePetInfoRepository extends JpaRepository<PlacePetInfo, Long> {
    Optional<PlacePetInfo> findByPlaceId(Long placeId);
}
