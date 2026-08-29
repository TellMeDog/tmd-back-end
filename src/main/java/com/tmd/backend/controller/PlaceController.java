package com.tmd.backend.controller;

import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.place.PlaceDetailResponse;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "Place", description = "장소 검색 API")
@RequiredArgsConstructor
@RestController
@RequestMapping("/places")
public class PlaceController {
    private final PlaceService placeService;

    @Operation(
        summary = "카테고리별 장소 검색",
        description = "사용자가 보고 있는 지도 영역 내에서 선택한 카테고리에 해당하는 장소를 조회합니다." )
    @GetMapping("/search/category")
    public ResponseEntity<SuccessResponseDto<List<PlaceMarkerResponse>>> getPlacesWithCategory(
        @Parameter(description = "지도 영역의 남서쪽 위도", example = "37.1234") @RequestParam double swLat,
        @Parameter(description = "지도 영역의 남서쪽 경도", example = "127.1234") @RequestParam double swLng,
        @Parameter(description = "지도 영역의 북동쪽 위도", example = "38.1234") @RequestParam double neLat,
        @Parameter(description = "지도 영역의 북동쪽 경도", example = "128.1234") @RequestParam double neLng,
        @Parameter(description = "검색할 카테고리", example = "카페") @RequestParam String category,
        @Parameter(description = "조회할 반려동물 ID", example = "1") @RequestParam Long petId,
        @AuthenticationPrincipal(expression = "username") String email) {

        log.info("장소 목록 조회: bounds=({},{})~({},{}), petId={}, category={}", swLat, swLng, neLat, neLng, petId, category);

        List<PlaceMarkerResponse> response = placeService.searchByCategory(category, email, petId, swLat, swLng, neLat, neLng);

        return ResponseEntity.ok(SuccessResponseDto.success("장소 목록을 조회했습니다.", response));
    }

    @Operation(
        summary = "장소 검색",
        description = "장소명 또는 지역명을 키워드로 장소를 검색합니다."
    )
    @GetMapping("/search/keyword")
    public ResponseEntity<SuccessResponseDto<List<PlaceMarkerResponse>>> searchPlaces(
        @Parameter(description = "검색 키워드", example = "용산공원") @RequestParam String keyword,
        @Parameter(description = "조회할 반려동물 ID", example = "1") @RequestParam Long petId,
        @AuthenticationPrincipal(expression = "username") String email) {

        log.info("장소 검색: keyword={}, petID={}, email={}", keyword, petId, email);

        List<PlaceMarkerResponse> response = placeService.searchByKeyword(keyword, petId, email);

        return ResponseEntity.ok(SuccessResponseDto.success("장소 목록을 조회했습니다.", response));
    }

    @Operation(
        summary = "지역별 장소 검색",
        description = "시도 및 시군구 기준으로 해당 장소를 조회합니다."
    )
    @GetMapping("/region/{lDongRegnNm}/{lDongSignguNm}")
    public ResponseEntity<SuccessResponseDto<List<PlaceMarkerResponse>>> searchByRegion(
        @Parameter(description = "시도 이름", example = "서울특별시") @PathVariable String lDongRegnNm,
        @Parameter(description = "시군구 이름", example = "강남구") @PathVariable String lDongSignguNm,
        @Parameter(description = "조회할 반려동물 ID", example = "1") @RequestParam Long petId,
        @AuthenticationPrincipal(expression = "username") String email){
        log.info("지역 기반 장소 검색: 시도 이름 = {}, 시군구 이름 = {}, petId = {}, email = {}", lDongRegnNm, lDongSignguNm, petId, email);
        List<PlaceMarkerResponse> response = placeService.searchByRegion(lDongRegnNm, lDongSignguNm, petId, email);

        return ResponseEntity.ok(SuccessResponseDto.success("지역 기반 장소 목록을 조회했습니다.", response));
    }

    @Operation(
        summary = "장소 상세 조회",
        description = "장소의 상세 정보와 반려동물 기준 출입 정책 정보를 조회합니다."
    )
    @GetMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<PlaceDetailResponse>> getPlaceDetail(
        @Parameter(description = "장소의 경도", example = "127.1234") @RequestParam double mapX,
        @Parameter(description = "장소의 위도", example = "37.1234") @RequestParam double mapY,
        @Parameter(description = "장소 ID", example = "1") @PathVariable Long placeId,
        @Parameter(description = "조회할 반려동물 ID", example = "1")@RequestParam Long petId,
        @AuthenticationPrincipal(expression = "username") String email) {

        log.info("장소 상세 조회: placeId={}, petId={}", placeId, petId);

        PlaceDetailResponse response = placeService.getPlaceDetail(mapX, mapY, placeId, petId, email);

        return ResponseEntity.ok(SuccessResponseDto.success("장소 상세 정보를 조회했습니다.", response));
    }
}
