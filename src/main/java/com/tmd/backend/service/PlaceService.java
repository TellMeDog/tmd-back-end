package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.common.Region;
import com.tmd.backend.common.RegionDetail;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetInfo;
import com.tmd.backend.dto.response.place.PlaceDetailResponse;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.external.TourApiClient;
import com.tmd.backend.external.TourApiPetInfoItem;
import com.tmd.backend.external.TourApiPlaceItem;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.PlacePetInfoRepository;
import com.tmd.backend.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceService {
    private static final long MAX_RADIUS = 20_000;
    private static final double EARTH_RADIUS_M = 6_371_000;

    private final GridService gridService;
    private final PlaceRepository placeRepository;
    private final PetRepository petRepository;
    private final TourApiClient tourApiClient;
    private final PlacePetInfoRepository placePetInfoRepository;
    private final MarkerColorService markerColorService;
    private final KeywordCacheService keywordCacheService;

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

    // 숙소, 공원, 계곡등과 같이 우리가 정해놓은 버튼을 클릭했을때 동작
    // 현재 화면 내에서 검색
    public List<PlaceMarkerResponse> searchByCategory(String category, String email, Long petId, double swLat, double swLng, double neLat, double neLng){

        String radius = calculateRadius(swLat, swLng, neLat, neLng);
        List<double[]> calculate = gridService.calculate(swLat, swLng, neLat, neLng);
        List<double[]> unfetched = gridService.findUnfetchedCells(calculate);
        List<CategoryCode> codes = CATEGORY_MAP.get(category);

        if(codes == null){
            throw new BaseException(ErrorCode.INVALID_CATEGORY);
        }

        for (double[] cell : unfetched) {
            fetchAndCacheRegionByCategory(cell[0], cell[1], radius, codes);
        }


        Pet pet = petRepository.findByIdAndUserEmail(petId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.NOT_OWNER_OF_DOG));

        return codes.stream()
            .flatMap(code ->
                placeRepository.findPlacesWithCategory(swLat, swLng, neLat, neLng, code.lclsSystm1, code.lclsSystm2, code.lclsSystm3).stream())
            .map(place -> toMarkerResponse(place, pet))
            .toList();
    }

    // 검색창에 검색 로직
    public List<PlaceMarkerResponse> searchByKeyword(String keyword, Long petId, String email){
        if(!keywordCacheService.isFetched(keyword)){
            List<TourApiPlaceItem> items = tourApiClient.getPlacesByKeyword(keyword);
            for(TourApiPlaceItem item : items){
                placeRepository.findByContentId(item.getContentid())
                    .orElseGet(() -> placeRepository.save(Place.from(item)));
            }
            keywordCacheService.markFetched(keyword);
        }
        List<Place> places = placeRepository.findPlacesWithKeyword(keyword);

        Pet pet = petRepository.findByIdAndUserEmail(petId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.NOT_OWNER_OF_DOG));

        return places.stream()
            .map(place -> toMarkerResponse(place, pet))
            .toList();
    }

    // 지역별 검색 기능
    public List<PlaceMarkerResponse> searchByRegion(String lDongRegnNm, String lDongSignguNm, Long petId, String email){
        Pet pet = petRepository.findByIdAndUserEmail(petId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.NOT_OWNER_OF_DOG));

        Region region = Region.fromName(lDongRegnNm);
        String lDongRegnCd = region.getCode();
        RegionDetail regionDetail = RegionDetail.fromName(region, lDongSignguNm);
        String lDongSignguCd = regionDetail.getCode();

        return placeRepository.findPlacesByRegion(lDongRegnCd, lDongSignguCd).stream()
            .map(place -> toMarkerResponse(place, pet))
            .toList();
    }

    // 마커(Place) 클릭시 엔드포인트
    public PlaceDetailResponse getPlaceDetail(double mapX, double mapY, Long placeId, Long petId, String email){
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new BaseException(ErrorCode.PLACE_NOT_FOUND));

        Pet pet = petRepository.findByIdAndUserEmail(petId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.NOT_OWNER_OF_DOG));

        PlacePetInfo info = ensurePetInfoLoaded(place);

        String markerColor = markerColorService.calculateMarkerColor(info, pet);

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

        PlaceDetailResponse.VisitStats mockVisitStats = PlaceDetailResponse.VisitStats.builder()
            .enteredCount(100)
            .mismatchedCount(10)
            .deniedCount(1)
            .lastReportedAt("2026-08-23").build();

        String dist = String.valueOf(calculateDistance(mapX, mapY, place.getMapX(), place.getMapY()));

        return PlaceDetailResponse.builder()
            .placeId(placeId)
            .contentId(place.getContentId())
            .zipCode(place.getZipCode())
            .addr1(place.getAddr1())
            .addr2(place.getAddr2())
            .title(place.getTitle())
            .mapX(place.getMapX())
            .mapY(place.getMapY())
            .firstImage(place.getFirstImage())
            .firstImage2(place.getFirstImage2())
            .dist(dist)
            .modifiedTime(place.getModifiedTime())
            .markerColor(markerColor)
            .petPolicyInfo(petPolicyInfo)
            .isFavorite(true) // TODO: 즐겨찾기 로직 후 변경
            .averageRating(4.0) // TODO: 리뷰 로직 후 변경
            .visitStats(mockVisitStats).build();
    }

    private void fetchAndCacheRegionByCategory(double gridLat, double gridLng, String radius, List<CategoryCode> codes) {
        //TODO: ApiCallLimiter 적용
        for(CategoryCode code : codes){
            List<TourApiPlaceItem> items = tourApiClient.getLocationBasedListByCategory(gridLng, gridLat, radius, code.lclsSystm1, code.lclsSystm2, code.lclsSystm3);
            for (TourApiPlaceItem item : items) {
                placeRepository.findByContentId(item.getContentid())
                    .orElseGet(() -> placeRepository.save(Place.create(
                        item.getContentid(), item.getZipCode(), item.getAddr1(),item.getAddr1(), item.getTitle(),
                        Double.parseDouble(item.getMapx()), Double.parseDouble(item.getMapy()),
                        item.getFirstImage(), item.getFirstImage2(), item.getModifiedTime(),
                        item.getLDongRegnCd(), item.getLDongSignguCd(),
                        item.getLclsSystm1(), item.getLclsSystm2(), item.getLclsSystm3()
                    )));
                // PlacePetInfo는 여기서 호출 안 함 — 배너/상세 조회 시점에 지연 로딩
            }
        }
        gridService.markFetched(gridLat, gridLng);
    }

    private PlaceMarkerResponse toMarkerResponse(Place place, Pet pet) {
        String color = markerColorService.calculateMarkerColor(place.getPlacePetInfo(), pet);
        return PlaceMarkerResponse.builder()
            .placeId(place.getId())
            .mapX(place.getMapX())
            .mapY(place.getMapY())
            .markerColor(color)
            .build();
    }

    private PlacePetInfo ensurePetInfoLoaded(Place place) {
        if (place.getPlacePetInfo() != null) {
            return place.getPlacePetInfo();
        }
        TourApiPetInfoItem item = tourApiClient.getPetTourInfo(place.getContentId());
        if (item == null) return null;

        PlacePetInfo info = PlacePetInfo.from(place, item);
        placePetInfoRepository.save(info);
        return info;
    }

    private String calculateRadius(double swLat, double swLng, double neLat, double neLng) {
        double centerLat = (swLat + neLat) / 2;
        double centerLng = (swLng + neLng) / 2;

        long radius = calculateDistance(centerLat, centerLng, neLat, neLng);

        if (radius > MAX_RADIUS) {
            throw new BaseException(ErrorCode.INVALID_MAP_BOUNDS);
        }

        return String.valueOf(radius);
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
}
