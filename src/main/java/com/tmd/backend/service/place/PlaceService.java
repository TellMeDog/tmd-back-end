package com.tmd.backend.service.place;

import com.tmd.backend.service.favorite.FavoriteService;
import com.tmd.backend.service.review.ReviewService;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.common.MarkerColor;
import com.tmd.backend.common.PlaceSearchCategory;
import com.tmd.backend.common.Region;
import com.tmd.backend.common.RegionDetail;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetInfo;
import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.place.PlaceCategoryResponse;
import com.tmd.backend.dto.response.place.PlaceDetailResponse;
import com.tmd.backend.dto.response.place.PlaceMapMarkerResponse;
import com.tmd.backend.dto.response.place.PlaceMapSearchResponse;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.dto.response.place.PlaceSearchCategoryResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.pet.PetRepository;
import com.tmd.backend.repository.place.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private final PlaceCategoryService placeCategoryService;

    // 프론트엔드가 현재 화면보다 넓게 계산한 bbox를 전달한다.
    // 백엔드는 전달받은 bbox를 그대로 조회하며 화면 이동에 따른 재검색 여부는 프론트엔드가 판단한다.
    public PlaceMapSearchResponse searchByCategory(
        String category,
        String email,
        Long petId,
        double swLat,
        double swLng,
        double neLat,
        double neLng
    ) {
        validateMapBounds(swLat, swLng, neLat, neLng);
        Pet pet = findPetIfProvided(petId, email);
        CategoryFilter categoryFilter = resolveCategoryFilter(category);

        List<Place> places = categoryFilter == null
            ? placeRepository.findPlacesWithinBounds(swLat, swLng, neLat, neLng)
            : placeRepository.findPlacesWithCategory(
                swLat,
                swLng,
                neLat,
                neLng,
                categoryFilter.depth(),
                categoryFilter.code()
            );

        List<PlaceMapMarkerResponse> markers = toMapMarkerResponses(places, pet).stream()
            .sorted(Comparator.comparing(PlaceMapMarkerResponse::placeId))
            .toList();
        return PlaceMapSearchResponse.from(markers);
    }

    public PageResponse<PlaceMarkerResponse> searchCategoryList(
        String category,
        String email,
        Long petId,
        double swLat,
        double swLng,
        double neLat,
        double neLng,
        double currMapX,
        double currMapY,
        int page,
        int size
    ) {
        validateMapBounds(swLat, swLng, neLat, neLng);
        validateCoordinates(currMapY, currMapX);
        validatePageRequest(page, size);
        Pet pet = findPetIfProvided(petId, email);
        CategoryFilter categoryFilter = resolveCategoryFilter(category);
        PageRequest pageable = PageRequest.of(page, size);
        Page<Place> places = categoryFilter == null
            ? placeRepository.findPlacePageWithinBounds(
                swLat, swLng, neLat, neLng, currMapX, currMapY, pageable
            )
            : placeRepository.findPlacePageWithCategory(
                swLat, swLng, neLat, neLng, categoryFilter.depth(), categoryFilter.code(),
                currMapX, currMapY, pageable
            );
        return toPlacePageResponse(places, email, pet, currMapX, currMapY);
    }

    public List<PlaceCategoryResponse> getCategories() {
        return placeCategoryService.getCategories();
    }

    public List<PlaceSearchCategoryResponse> getSearchCategories() {
        return placeCategoryService.getSearchCategories();
    }

    // 검색창에 검색 로직
    public PlaceMapSearchResponse searchByKeyword(String keyword, Long petId, String email) {
        String trimmedKeyword = validateKeyword(keyword);
        Pet pet = findPetIfProvided(petId, email);

        List<Place> places = placeRepository.findPlacesWithKeyword(trimmedKeyword);
        List<PlaceMapMarkerResponse> markers = toMapMarkerResponses(places, pet).stream()
            .sorted(Comparator.comparing(PlaceMapMarkerResponse::placeId))
            .toList();
        return PlaceMapSearchResponse.from(markers);
    }

    public PageResponse<PlaceMarkerResponse> searchKeywordList(
        String keyword,
        Long petId,
        String email,
        double currMapX,
        double currMapY,
        int page,
        int size
    ) {
        String trimmedKeyword = validateKeyword(keyword);
        validateCoordinates(currMapY, currMapX);
        validatePageRequest(page, size);
        Pet pet = findPetIfProvided(petId, email);
        Page<Place> places = placeRepository.findPlacePageWithKeyword(
            trimmedKeyword, currMapX, currMapY, PageRequest.of(page, size)
        );
        return toPlacePageResponse(places, email, pet, currMapX, currMapY);
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
        double mapX,
        double mapY,
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
                place,
                pet,
                mapX,
                mapY,
                reviewService.getAverageRating(placeId),
                StringUtils.hasText(email) && favoriteService.isFavorite(email, placeId)
            ))
            .zipCode(place.getZipCode())
            .addr1(place.getAddr1())
            .addr2(place.getAddr2())
            .firstImage2(place.getFirstImage2())
            .modifiedTime(place.getModifiedTime())
            .placeType(placeType(place))
            .tel(place.getTel())
            .businessStatus(place.getBusinessStatus())
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
        List<Long> placeIds = places.stream().map(Place::getId).toList();
        Map<Long, Double> averageRatings = reviewService.getAverageRatings(placeIds);
        Set<Long> favoritePlaceIds = StringUtils.hasText(email)
            ? favoriteService.getFavoritePlaceIds(email, placeIds)
            : Set.of();
        return places.stream()
            .map(place -> toMarkerResponse(
                place,
                pet,
                currMapX,
                currMapY,
                averageRatings.getOrDefault(place.getId(), 0.0),
                favoritePlaceIds.contains(place.getId())
            ))
            .toList();
    }

    private PlaceMarkerResponse toMarkerResponse(
        Place place,
        Pet pet,
        double currMapX,
        double currMapY,
        double averageRating,
        boolean isFavorite
    ) {
        MarkerColor color = markerColorService.calculateMarkerColorForPlace(place, pet);
        long distance = calculateDistance(currMapY, currMapX, place.getMapY(), place.getMapX());
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
            .placeType(placeType(place))
            .build();
    }

    private List<PlaceMapMarkerResponse> toMapMarkerResponses(List<Place> places, Pet pet) {
        return places.stream()
            .map(place -> new PlaceMapMarkerResponse(
                place.getId(),
                place.getMapX(),
                place.getMapY(),
                markerColorService.calculateMarkerColorForPlace(place, pet).name(),
                placeType(place)
            ))
            .toList();
    }

    private PageResponse<PlaceMarkerResponse> toPlacePageResponse(
        Page<Place> places,
        String email,
        Pet pet,
        double currMapX,
        double currMapY
    ) {
        List<PlaceMarkerResponse> content = toMarkerResponses(
            places.getContent(), email, pet, currMapX, currMapY
        );
        return new PageResponse<>(
            content,
            places.getNumber(),
            places.getSize(),
            places.getTotalElements(),
            places.getTotalPages(),
            places.hasNext()
        );
    }

    private String placeType(Place place) {
        return place.isAnimalHospital() ? "ANIMAL_HOSPITAL" : "TOUR";
    }

    private CategoryFilter resolveCategoryFilter(String category) {
        if (!StringUtils.hasText(category)) return null;

        PlaceSearchCategory searchCategory = placeCategoryService.getSearchCategory(category);
        return searchCategory.isAll()
            ? null
            : new CategoryFilter(searchCategory.getDepth(), searchCategory.getQueryCode());
    }

    private String validateKeyword(String keyword) {
        String trimmedKeyword = keyword == null ? "" : keyword.trim();
        if (trimmedKeyword.isBlank()) {
            throw new BaseException(ErrorCode.PLACE_NOT_FOUND);
        }
        return trimmedKeyword;
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BaseException(ErrorCode.VALIDATION_ERROR);
        }
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

    private record CategoryFilter(int depth, String code) {}
}
