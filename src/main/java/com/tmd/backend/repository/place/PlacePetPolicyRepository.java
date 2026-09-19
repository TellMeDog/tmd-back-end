package com.tmd.backend.repository.place;

import com.tmd.backend.domain.place.PlacePetPolicy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlacePetPolicyRepository extends JpaRepository<PlacePetPolicy, Long> {
    Optional<PlacePetPolicy> findByPlaceId(Long placeId);
}
