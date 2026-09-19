package com.tmd.backend.repository.place;

import com.tmd.backend.domain.place.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface PlaceRepositoryCustom {

    List<Place> findPlacesWithinBounds(double swLat, double swLng, double neLat, double neLng);

    List<Place> findPlacesWithCategory(double swLat, double swLng, double neLat, double neLng,
                                       int categoryDepth, String categoryCode);
    List<Place> findPlacesWithKeyword(String keyword);

    Page<Place> findPlacePageWithinBounds(double swLat, double swLng, double neLat, double neLng,
                                          double currMapX, double currMapY, Pageable pageable);

    Page<Place> findPlacePageWithCategory(double swLat, double swLng, double neLat, double neLng,
                                          int categoryDepth, String categoryCode,
                                          double currMapX, double currMapY, Pageable pageable);

    Page<Place> findPlacePageWithKeyword(String keyword, double currMapX, double currMapY,
                                         Pageable pageable);

    List<Place> findPlacesByRegion(String lDongRegnCd, String lDongSignguCd);

    List<Place> findPlacesWithoutPetInfo(int limit);
}
