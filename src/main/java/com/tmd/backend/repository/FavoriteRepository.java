package com.tmd.backend.repository;

import com.tmd.backend.domain.favorite.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndPlaceId(Long userId, Long placeId);

    boolean existsByUserIdAndPlaceId(Long userId, Long placeId);

    @EntityGraph(attributePaths = {"place", "place.placePetPolicy"})
    Page<Favorite> findAllByUserId(Long userId, Pageable pageable);

    @Query("SELECT f.place.id FROM Favorite f " +
        "WHERE f.user.email = :email AND f.place.id IN :placeIds")
    List<Long> findPlaceIdsByUserEmailAndPlaceIdIn(
        @Param("email") String email,
        @Param("placeIds") Collection<Long> placeIds
    );
}
