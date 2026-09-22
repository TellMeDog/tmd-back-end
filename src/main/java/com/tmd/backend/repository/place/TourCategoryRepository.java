package com.tmd.backend.repository.place;

import com.tmd.backend.domain.place.TourCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TourCategoryRepository extends JpaRepository<TourCategory, String> {
    List<TourCategory> findAllByActiveTrueOrderByDepthAscDisplayOrderAscCodeAsc();
}
