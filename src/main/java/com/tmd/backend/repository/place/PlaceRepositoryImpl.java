package com.tmd.backend.repository.place;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlaceSource;
import com.tmd.backend.domain.place.QPlace;
import com.tmd.backend.domain.place.QPlacePetInfo;
import com.tmd.backend.domain.place.QPlacePetPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@RequiredArgsConstructor
@Repository
public class PlaceRepositoryImpl implements PlaceRepositoryCustom{

    private final JPAQueryFactory jpaQueryFactory;

    private static final QPlace place = QPlace.place;
    private static final QPlacePetInfo placePetInfo = QPlacePetInfo.placePetInfo;
    private static final QPlacePetPolicy placePetPolicy = QPlacePetPolicy.placePetPolicy;

    @Override
    public List<Place> findPlacesWithinBounds(double swLat, double swLng, double neLat, double neLng) {
        return jpaQueryFactory
            .selectFrom(place)
            .leftJoin(place.placePetPolicy, placePetPolicy).fetchJoin()
            .where(
                activePlace(),
                place.mapX.between(swLng, neLng),
                place.mapY.between(swLat, neLat)
            )
            .fetch();
    }

    @Override
    public List<Place> findPlacesWithCategory(double swLat, double swLng, double neLat, double neLng,
                                              int categoryDepth, String categoryCode) {
        return jpaQueryFactory
            .selectFrom(place)
            .leftJoin(place.placePetPolicy, placePetPolicy).fetchJoin()
            .where(
                activePlace(),
                place.mapX.between(swLng, neLng),
                place.mapY.between(swLat, neLat),
                categoryEq(categoryDepth, categoryCode))
            .fetch();
    }

    @Override
    public List<Place> findPlacesWithKeyword(String keyword) {
        return jpaQueryFactory
            .selectFrom(place)
            .leftJoin(place.placePetPolicy, placePetPolicy).fetchJoin()
            .where(activePlace(), place.title.contains(keyword))
            .fetch();
    }

    @Override
    public Page<Place> findPlacePageWithinBounds(double swLat, double swLng, double neLat, double neLng,
                                                 double currMapX, double currMapY, Pageable pageable) {
        return findPage(
            currMapX,
            currMapY,
            pageable,
            activePlace(),
            place.mapX.between(swLng, neLng),
            place.mapY.between(swLat, neLat)
        );
    }

    @Override
    public Page<Place> findPlacePageWithCategory(double swLat, double swLng, double neLat, double neLng,
                                                 int categoryDepth, String categoryCode,
                                                 double currMapX, double currMapY, Pageable pageable) {
        return findPage(
            currMapX,
            currMapY,
            pageable,
            activePlace(),
            place.mapX.between(swLng, neLng),
            place.mapY.between(swLat, neLat),
            categoryEq(categoryDepth, categoryCode)
        );
    }

    @Override
    public Page<Place> findPlacePageWithKeyword(String keyword, double currMapX, double currMapY,
                                                Pageable pageable) {
        return findPage(
            currMapX,
            currMapY,
            pageable,
            activePlace(),
            place.title.contains(keyword)
        );
    }

    @Override
    public List<Place> findPlacesByRegion(String lDongRegnCd, String lDongSignguCd) {
        return jpaQueryFactory
            .selectFrom(place)
            .leftJoin(place.placePetInfo, placePetInfo).fetchJoin()
            .where(
                activePlace(),
                lDongRegnCdEq(lDongRegnCd),
                lDongSignguCdEq(lDongSignguCd)
            )
            .fetch();
    }

    @Override
    public List<Place> findPlacesWithoutPetInfo(int limit) {
        return jpaQueryFactory
            .selectFrom(place)
            .leftJoin(placePetInfo).on(placePetInfo.place.eq(place))
            .where(activePlace(), tourApiPlace(), placePetInfo.id.isNull())
            .orderBy(place.id.asc())
            .limit(limit)
            .fetch();
    }

    private BooleanExpression lDongRegnCdEq(String lDongRegnCd){
        return lDongRegnCd == null
            ? null
            : place.lDongRegnCd.eq(lDongRegnCd);
    }

    private BooleanExpression activePlace() {
        return place.active.isTrue().or(place.active.isNull());
    }

    private BooleanExpression tourApiPlace() {
        return place.source.eq(PlaceSource.TOUR_API).or(place.source.isNull());
    }

    private BooleanExpression lDongSignguCdEq(String lDongSignguCd){
        return lDongSignguCd == null
            ? null
            : place.lDongSignguCd.eq(lDongSignguCd);
    }

    private BooleanExpression categoryEq(int depth, String code) {
        return switch (depth) {
            case 1 -> place.lclsSystm1.eq(code);
            case 2 -> place.lclsSystm2.eq(code);
            case 3 -> place.lclsSystm3.eq(code);
            default -> throw new IllegalArgumentException("Category depth must be between 1 and 3");
        };
    }

    private Page<Place> findPage(double currMapX, double currMapY, Pageable pageable,
                                 BooleanExpression... predicates) {
        NumberExpression<Double> distance = distanceExpression(currMapX, currMapY);
        List<Place> content = jpaQueryFactory
            .selectFrom(place)
            .leftJoin(place.placePetPolicy, placePetPolicy).fetchJoin()
            .where(predicates)
            .orderBy(distance.asc(), place.id.asc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        Long total = jpaQueryFactory
            .select(place.count())
            .from(place)
            .where(predicates)
            .fetchOne();
        return new PageImpl<>(content, pageable, total == null ? 0 : total);
    }

    private NumberExpression<Double> distanceExpression(double currMapX, double currMapY) {
        return Expressions.numberTemplate(
            Double.class,
            "6371000.0 * acos(least(1.0, greatest(-1.0, " +
                "cos(radians({0})) * cos(radians({1})) * " +
                "cos(radians({2}) - radians({3})) + " +
                "sin(radians({0})) * sin(radians({1})))))",
            currMapY,
            place.mapY,
            place.mapX,
            currMapX
        );
    }
}
