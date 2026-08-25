package com.tmd.backend.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.QPlace;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class PlaceRepositoryImpl implements PlaceRepositoryCustom{
    private final JPAQueryFactory jpaQueryFactory;

    QPlace place = QPlace.place; // 1. private final 2. static 고려

    @Override
    public List<Place> findPlacesWithCategory(double swLat, double swLng, double neLat, double neLng,
                                              String lclsSystm1, String lclsSystm2, String lclsSystm3) {
        return jpaQueryFactory
            .selectFrom(place)
            .where(
                place.mapX.between(swLng, neLng),
                place.mapY.between(swLat, neLat),
                lclsSystm1Eq(lclsSystm1),
                lclsSystm2Eq(lclsSystm2),
                lclsSystm3Eq(lclsSystm3))
            .fetch();
    }

    private BooleanExpression lclsSystm1Eq(String lclsSystm1){
        return lclsSystm1 == null
            ? null
            : place.lclsSystm1.eq(lclsSystm1);
    }

    private BooleanExpression lclsSystm2Eq(String lclsSystm2){
        return lclsSystm2 == null
            ? null
            : place.lclsSystm2.eq(lclsSystm2);
    }
    private BooleanExpression lclsSystm3Eq(String lclsSystm3){
        return lclsSystm3 == null
            ? null
            : place.lclsSystm3.eq(lclsSystm3);
    }
}
