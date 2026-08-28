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

    private static final QPlace place = QPlace.place;

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

    @Override
    public List<Place> findPlacesWithKeyword(String keyword) {
        return jpaQueryFactory
            .selectFrom(place)
            .where(
                place.addr1.contains(keyword).or(place.addr2.contains(keyword)) .or(place.title.contains(keyword))
            )
            .fetch();
    }

    @Override
    public List<Place> findPlacesByRegion(String lDongRegnCd, String lDongSignguCd){
        return jpaQueryFactory
            .selectFrom(place)
            .where(
                lDongRegnCdEq(lDongRegnCd),
                lDongSignguCdEq(lDongSignguCd)
            )
            .fetch();
    }

    private BooleanExpression lDongRegnCdEq(String lDongRegnCd){
        return lDongRegnCd == null
            ? null
            : place.lDongRegnCd.eq(lDongRegnCd);
    }

    private BooleanExpression lDongSignguCdEq(String lDongSignguCd){
        return lDongSignguCd == null
            ? null
            : place.lDongSignguCd.eq(lDongSignguCd);
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
