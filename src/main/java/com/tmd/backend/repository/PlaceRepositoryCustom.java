package com.tmd.backend.repository;

import com.tmd.backend.domain.place.Place;

import java.util.List;

public interface PlaceRepositoryCustom {

    List<Place> findPlacesWithinBounds(double swLat, double swLng, double neLat, double neLng);

    List<Place> findPlacesWithCategory(double swLat, double swLng, double neLat, double neLng,
                                       int categoryDepth, String categoryCode);
    List<Place> findPlacesWithKeyword(String keyword);

    List<Place> findPlacesByRegion(String lDongRegnCd, String lDongSignguCd);

    List<Place> findPlacesWithoutPetInfo(int limit);
}
