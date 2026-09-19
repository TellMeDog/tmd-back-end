package com.tmd.backend.repository;

import com.tmd.backend.domain.place.Place;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long>, PlaceRepositoryCustom {
    Optional<Place> findByContentId(String contentId);
    List<Place> findByTitleContaining(String keyword);
    List<Place> findTop100ByIdGreaterThanOrderByIdAsc(Long lastId);

    @EntityGraph(attributePaths = {"placePetInfo", "placePetPolicy"})
    @Query("SELECT p FROM Place p")
    List<Place> findAllForSync();

    @Query("SELECT p.id FROM Place p WHERE (p.active = true OR p.active IS NULL) " +
        "AND (p.petInfoSyncedModifiedTime IS NULL OR p.petInfoSyncedModifiedTime <> p.modifiedTime) ORDER BY p.id")
    List<Long> findPendingPetInfoSyncIds();

    @Query("SELECT DISTINCT p.lclsSystm1, p.lclsSystm2, p.lclsSystm3 FROM Place p " +
        "WHERE (p.active = true OR p.active IS NULL)")
    List<Object[]> findDistinctActiveCategoryPaths();
}
