package com.tmd.backend.repository.place;

import com.tmd.backend.domain.place.PlacePetPolicyReview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlacePetPolicyReviewRepository extends JpaRepository<PlacePetPolicyReview, Long> {
    boolean existsByPlaceIdAndSourceModifiedTime(Long placeId, String sourceModifiedTime);
}
