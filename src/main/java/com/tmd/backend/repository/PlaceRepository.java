package com.tmd.backend.repository;

import com.tmd.backend.domain.place.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long>, PlaceRepositoryCustom {
    Optional<Place> findByContentId(String contentId);
    List<Place> findTop100ByIdGreaterThanOrderByIdAsc(Long lastId);
}
