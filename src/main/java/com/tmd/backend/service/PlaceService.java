package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.common.MarkerColor;
import com.tmd.backend.common.Region;
import com.tmd.backend.common.RegionDetail;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetInfo;
import com.tmd.backend.dto.response.place.PlaceDetailResponse;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {
    private static final double EARTH_RADIUS_M = 6_371_000;
    private static final double HOME_RADIUS_M = 6_000;
    private static final int HOME_PLACE_LIMIT = 10;

    private final PlaceRepository placeRepository;
    private final PetRepository petRepository;
    private final MarkerColorService markerColorService;
    private final ReviewService reviewService;
    private final FavoriteService favoriteService;

    // Key Enum으로 할지 고려
    private static final Map<String, List<CategoryCode>> CATEGORY_MAP = Map.of(
        "카페", List.of(new CategoryCode("FD", "FD05", null)),
        "계곡", List.of(new CategoryCode("NA", "NA01", "NA010400")),
        "숙소", List.of(new CategoryCode("AC", null, null)),
        "음식점", List.of(
            new CategoryCode("FD", "FD01", null),
            new CategoryCode("FD", "FD02", null),
            new CategoryCode("FD", "FD03", null)
        ),
        "주점", List.of(new CategoryCode("FD", "FD04", null)
        )
    );

    private record CategoryCode(String lclsSystm1, String lclsSystm2, String lclsSystm3){}

    // 프론트엔드가 현재 화면보다 넓게 계산한 bbox를 전달한다.
    // 백엔드는 전달받은 bbox를 그대로 조회하며 화면 이동에 따른 재검색 여부는 프론트엔드가 판단한다.
    public List<PlaceMarkerResponse> searchByCategory(
        String category,
        String email,
        Long petId,
        double swLat,
        double swLng,
        double neLat,
        double neLng,
        double currMapX,
        double currMapY
    ) {
        String trimmedCategory = category == null ? "" : category.trim();
        boolean allCategories = "전체".equals(trimmedCategory);
        List<CategoryCode> categoryCodes = allCategories ? List.of() : CATEGORY_MAP.get(trimmedCategory);
        if (!allCategories && categoryCodes == null) {
            throw new BaseException(ErrorCode.INVALID_CATEGORY);
        }

        validateMapBounds(swLat, swLng, neLat, neLng);
        validateCoordinates(currMapY, currMapX);
        Pet pet = findPetIfProvided(petId, email);

        List<Place> places = allCategories
            ? placeRepository.findPlacesWithinBounds(swLat, swLng, neLat, neLng)
            : findPlacesByCategoryCodes(categoryCodes, swLat, swLng, neLat, neLng);

        return toMarkerResponses(places, email, pet, currMapX, currMapY).stream()
            .sorted(Comparator
                .comparingLong(PlaceMarkerResponse::getDistance)
                .thenComparing(PlaceMarkerResponse::getPlaceId))
            .toList();
    }

    private List<Place> findPlacesByCategoryCodes(
        List<CategoryCode> codes,
        double swLat,
        double swLng,
        double neLat,
        double neLng
    ) {
        return codes.stream()
            .flatMap(code -> placeRepository.findPlacesWithCategory(
                swLat,
                swLng,
                neLat,
                neLng,
                code.lclsSystm1(),
                code.lclsSystm2(),
                code.lclsSystm3()
            ).stream())
            .collect(Collectors.toMap(
                Place::getId,
                Function.identity(),
                (first, duplicate) -> first,
                LinkedHashMap::new
            ))
            .values()
            .stream()
            .toList();
    }

    // 검색창에 검색 로직
    public List<PlaceMarkerResponse> searchByKeyword(String keyword, Long petId, String email, double currMapX, double currMapY) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();

        if(trimmedKeyword.isBlank()) {
            throw new BaseException(ErrorCode.PLACE_NOT_FOUND); //TODO: 적절한 ErrorCode 추가할 것.
        }

        Pet pet = findPetIfProvided(petId, email);

        List<Place> places = placeRepository.findPlacesWithKeyword(trimmedKeyword);
        return toMarkerResponses(places, email, pet, currMapX, currMapY).stream()
            .sorted(Comparator
                .comparingLong(PlaceMarkerResponse::getDistance) // 거리순 정렬
                .thenComparing(PlaceMarkerResponse::getPlaceId)) // 거리가 같다면 id로 정렬
            .toList();
    }

    // 지역별 검색 기능
    public List<PlaceMarkerResponse> searchByRegion(String lDongRegnNm, String lDongSignguNm, Long petId, String email, double currMapX, double currMapY){
        Pet pet = findPetIfProvided(petId, email);

        Region region = Region.fromName(lDongRegnNm);
        String lDongRegnCd = region.getCode();
        RegionDetail regionDetail = RegionDetail.fromName(region, lDongSignguNm);
        String lDongSignguCd = regionDetail.getCode();

        List<Place> places = placeRepository.findPlacesByRegion(lDongRegnCd, lDongSignguCd);
        return toMarkerResponses(places, email, pet, currMapX, currMapY);
    }

    public List<PlaceMarkerResponse> init(String email, Long petId, double currMapX, double currMapY) {
        validateCoordinates(currMapY, currMapX);
        Pet pet = findPetIfProvided(petId, email);

        MapBounds bounds = calculateBounds(currMapY, currMapX, HOME_RADIUS_M);
        List<Place> places = placeRepository.findPlacesWithinBounds(
                bounds.swLat(),
                bounds.swLng(),
                bounds.neLat(),
                bounds.neLng()
            ).stream()
            .filter(place -> calculateDistance(
                currMapY,
                currMapX,
                place.getMapY(),
                place.getMapX()
            ) <= HOME_RADIUS_M)
            .sorted(Comparator
                .comparingLong((Place place) -> calculateDistance(
                    currMapY,
                    currMapX,
                    place.getMapY(),
                    place.getMapX()
                ))
                .thenComparing(Place::getId))
            .limit(HOME_PLACE_LIMIT)
            .toList();

        return toMarkerResponses(places, email, pet, currMapX, currMapY);
    }

    // 마커(Place) 클릭시 엔드포인트
    public PlaceDetailResponse getPlaceDetail(
        Long placeId,
        Long petId,
        String email,
        double currMapX,
        double currMapY,
        int reviewSize,
        String reviewSort
    ) {
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new BaseException(ErrorCode.PLACE_NOT_FOUND));
        if (!place.isActive()) {
            throw new BaseException(ErrorCode.PLACE_NOT_FOUND);
        }

        Pet pet = findPetIfProvided(petId, email);

        PlacePetInfo info = place.getPlacePetInfo();

        PlaceDetailResponse.PetPolicyInfo petPolicyInfo = (info == null)
            ? null
            : PlaceDetailResponse.PetPolicyInfo.builder()
            .acmpyTypeCd(info.getAcmpyTypeCd())
            .acmpyPsblCpam(info.getAcmpyPsblCpam())
            .acmpyNeedMtr(info.getAcmpyNeedMtr())
            .relaAcdntRiskMtr(info.getRelaAcdntRiskMtr())
            .relaPosesFclty(info.getRelaPosesFclty())
            .relaFrnshPrdlst(info.getRelaFrnshPrdlst())
            .relaPurcPrdlst(info.getRelaPurcPrdlst())
            .relaRntlPrdlst(info.getRelaRntlPrdlst())
            .etcAcmpyInfo(info.getEtcAcmpyInfo())
            .build();

        PlaceDetailResponse.VisitStats visitStats = reviewService.getVisitStats(placeId);

        return PlaceDetailResponse.builder()
            .placeMarkerResponse(toMarkerResponse(
                email,
                place,
                pet,
                currMapX,
                currMapY,
                reviewService.getAverageRating(placeId)
            ))
            .zipCode(place.getZipCode())
            .addr1(place.getAddr1())
            .addr2(place.getAddr2())
            .firstImage2(place.getFirstImage2())
            .modifiedTime(place.getModifiedTime())
            .petPolicyInfo(petPolicyInfo)
            .visitStats(visitStats)
            .myReviews(StringUtils.hasText(email)
                ? reviewService.getMyReviewListInPlace(placeId, email)
                : List.of())
            .reviews(reviewService.getPlaceReviewList(placeId, email, 0, reviewSize, reviewSort))
            .build();
    }

    private List<PlaceMarkerResponse> toMarkerResponses(
        List<Place> places,
        String email,
        Pet pet,
        double currMapX,
        double currMapY
    ) {
        Map<Long, Double> averageRatings = reviewService.getAverageRatings(
            places.stream().map(Place::getId).toList()
        );
        return places.stream()
            .map(place -> toMarkerResponse(
                email,
                place,
                pet,
                currMapX,
                currMapY,
                averageRatings.getOrDefault(place.getId(), 0.0)
            ))
            .toList();
    }

    private PlaceMarkerResponse toMarkerResponse(
        String email,
        Place place,
        Pet pet,
        double currMapX,
        double currMapY,
        double averageRating
    ) {
        MarkerColor color = markerColorService.calculateMarkerColor(place.getPlacePetPolicy(), pet);
        long distance = calculateDistance(currMapY, currMapX, place.getMapY(), place.getMapX());
        boolean isFavorite = StringUtils.hasText(email)
            && favoriteService.isFavorite(email, place.getId());
        return PlaceMarkerResponse.builder()
            .placeId(place.getId())
            .title(place.getTitle())
            .firstImage(place.getFirstImage())
            .mapX(place.getMapX())
            .mapY(place.getMapY())
            .distance(distance)
            .isFavorite(isFavorite) // TODO: 즐겨찾기 로직 후 변경
            .markerColor(color.name())
            .averageRating(averageRating)
            .build();
    }

    private Pet findPetIfProvided(Long petId, String email) {
        if (petId == null) {
            return null;
        }
        if (!StringUtils.hasText(email)) {
            throw new BaseException(ErrorCode.NOT_OWNER_OF_DOG);
        }
        return petRepository.findByIdAndUserEmail(petId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.NOT_OWNER_OF_DOG));
    }

    private void validateMapBounds(double swLat, double swLng, double neLat, double neLng) {
        validateCoordinates(swLat, swLng);
        validateCoordinates(neLat, neLng);
        if (swLat >= neLat || swLng >= neLng) {
            throw new BaseException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private void validateCoordinates(double lat, double lng) {
        if (!Double.isFinite(lat)
            || !Double.isFinite(lng)
            || lat < -90
            || lat > 90
            || lng < -180
            || lng > 180) {
            throw new BaseException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private MapBounds calculateBounds(double lat, double lng, double radiusM) {
        double angularDistance = radiusM / EARTH_RADIUS_M;
        double latDelta = Math.toDegrees(angularDistance);
        double cosine = Math.cos(Math.toRadians(lat));
        double lngDelta = Math.abs(cosine) < 1e-12
            ? 180
            : Math.min(180, Math.toDegrees(angularDistance / cosine));

        double swLat = Math.max(-90, lat - latDelta);
        double neLat = Math.min(90, lat + latDelta);
        double swLng = lng - lngDelta;
        double neLng = lng + lngDelta;
        if (swLng < -180 || neLng > 180) {
            swLng = -180;
            neLng = 180;
        }
        return new MapBounds(swLat, swLng, neLat, neLng);
    }

    private long calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        double latDistance = Math.toRadians(lat2 - lat1);
        double lngDistance = Math.toRadians(lng2 - lng1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
            + Math.cos(Math.toRadians(lat1))
            * Math.cos(Math.toRadians(lat2))
            * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return Math.round(EARTH_RADIUS_M * c);
    }

    private record MapBounds(double swLat, double swLng, double neLat, double neLng) {}
}
