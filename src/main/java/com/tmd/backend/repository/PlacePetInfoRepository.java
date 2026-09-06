package com.tmd.backend.repository;

import com.tmd.backend.domain.place.PlacePetInfo;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlacePetInfoRepository extends JpaRepository<PlacePetInfo, Long> {
    Optional<PlacePetInfo> findByPlaceId(Long placeId);

    @Query("SELECT p.place.id FROM PlacePetInfo p")
    List<Long> findAllProcessedPlaceIds();

    @Query("SELECT p FROM PlacePetInfo p " +
        "WHERE NOT EXISTS (SELECT pp FROM PlacePetPolicy pp WHERE pp.place = p.place) " +
        "ORDER BY p.id ASC")
    List<PlacePetInfo> findUnprocessed(Pageable pageable);
}

