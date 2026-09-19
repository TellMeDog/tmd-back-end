package com.tmd.backend.repository;

import com.tmd.backend.domain.place.TourCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TourCategoryRepository extends JpaRepository<TourCategory, String> {
    Optional<TourCategory> findByCodeAndActiveTrue(String code);

    List<TourCategory> findAllByActiveTrueOrderByDepthAscDisplayOrderAscCodeAsc();
}
