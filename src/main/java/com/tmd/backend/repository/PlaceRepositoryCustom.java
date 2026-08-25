package com.tmd.backend.repository;

import com.tmd.backend.domain.place.Place;

import java.util.List;

public interface PlaceRepositoryCustom {

    List<Place> findPlacesWithCategory(double swLat, double swLng, double neLat, double neLng,
                                       String lclsSystm1, String lclsSystm2, String lclsSystm3);
}
