package com.tmd.backend.controller;

import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.place.PlaceCategoryResponse;
import com.tmd.backend.dto.response.place.PlaceDetailResponse;
import com.tmd.backend.dto.response.place.PlaceMapSearchResponse;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.dto.response.place.PlaceSearchCategoryResponse;
import com.tmd.backend.service.place.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
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
        summary = "메인 화면 미니 지도에 표시",
        description = "메인 화면 지도에 표시될 장소들을 현재 좌표의 6km 이내의 10개 내려줍니다."
    )
    @GetMapping("/init")
    public ResponseEntity<SuccessResponseDto<List<PlaceMarkerResponse>>> init(
        @Parameter(description = "조회할 반려동물 ID (없으면 회색 마커)", example = "1")
        @RequestParam(required = false) Long petId,
        @Parameter(description = "사용자 현재 경도 좌표", example = "127.1234") @RequestParam double currMapX,
        @Parameter(description = "사용자 현재 위도 좌표", example = "37.1234") @RequestParam double currMapY,
        Authentication authentication
    ){
        String email = authenticatedEmail(authentication);

        List<PlaceMarkerResponse> response = placeService.init(email, petId, currMapX, currMapY);

        return ResponseEntity.ok(SuccessResponseDto.success("미니 지도 장소들을 조회했습니다.", response));
    }

    @Operation(
        summary = "카테고리별 장소 검색",
        description = "사용자가 보고 있는 지도 영역 내에서 선택한 한글 카테고리에 해당하는 장소를 조회합니다." )
    @GetMapping("/search/category")
    public ResponseEntity<SuccessResponseDto<PlaceMapSearchResponse>> getPlacesWithCategory(
        @Parameter(description = "지도 영역의 남서쪽 위도", example = "37.1234") @RequestParam double swLat,
        @Parameter(description = "지도 영역의 남서쪽 경도", example = "127.1234") @RequestParam double swLng,
        @Parameter(description = "지도 영역의 북동쪽 위도", example = "38.1234") @RequestParam double neLat,
        @Parameter(description = "지도 영역의 북동쪽 경도", example = "128.1234") @RequestParam double neLng,
        @Parameter(description = "검색할 한글 카테고리. 생략하거나 '전체'이면 전체 장소를 조회합니다.", example = "식당/카페")
        @RequestParam(required = false) String category,
        @Parameter(description = "조회할 반려동물 ID (없으면 회색 마커)", example = "1")
        @RequestParam(required = false) Long petId,
        Authentication authentication) {

        String email = authenticatedEmail(authentication);

        log.info("장소 목록 조회: bounds=({},{})~({},{}), petId={}, category={}",
            swLat, swLng, neLat, neLng, petId, category);

        PlaceMapSearchResponse response = placeService.searchByCategory(
            category,
            email,
            petId,
            swLat,
            swLng,
            neLat,
            neLng
        );

        return ResponseEntity.ok(SuccessResponseDto.success("장소 목록을 조회했습니다.", response));
    }

    @Operation(summary = "카테고리 검색 바텀시트 목록", description = "거리순으로 페이지 처리한 장소 목록을 조회합니다.")
    @GetMapping("/search/category/list")
    public ResponseEntity<SuccessResponseDto<PageResponse<PlaceMarkerResponse>>> getCategoryPlaceList(
        @RequestParam double swLat,
        @RequestParam double swLng,
        @RequestParam double neLat,
        @RequestParam double neLng,
        @RequestParam double currMapX,
        @RequestParam double currMapY,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Long petId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        String email = authenticatedEmail(authentication);
        PageResponse<PlaceMarkerResponse> response = placeService.searchCategoryList(
            category, email, petId, swLat, swLng, neLat, neLng,
            currMapX, currMapY, page, size
        );
        return ResponseEntity.ok(SuccessResponseDto.success("카테고리 장소 목록을 조회했습니다.", response));
    }

    @Operation(
        summary = "장소 카테고리 목록 조회",
        description = "활성 장소가 사용하는 카테고리를 계층형으로 조회합니다. Tour API 분류와 동물병원 카테고리를 포함합니다."
    )
    @GetMapping("/categories")
    public ResponseEntity<SuccessResponseDto<List<PlaceCategoryResponse>>> getCategories() {
        List<PlaceCategoryResponse> response = placeService.getCategories();
        return ResponseEntity.ok(SuccessResponseDto.success("장소 카테고리 목록을 조회했습니다.", response));
    }

    @Operation(
        summary = "검색용 카테고리 목록 조회",
        description = "장소 검색 API의 category 파라미터로 전달할 한글 카테고리 목록을 조회합니다."
    )
    @GetMapping("/search-categories")
    public ResponseEntity<SuccessResponseDto<List<PlaceSearchCategoryResponse>>> getSearchCategories() {
        List<PlaceSearchCategoryResponse> response = placeService.getSearchCategories();
        return ResponseEntity.ok(SuccessResponseDto.success("검색용 카테고리 목록을 조회했습니다.", response));
    }

    @Operation(
        summary = "장소 검색",
        description = "장소명 또는 지역명을 키워드로 장소를 검색합니다."
    )
    @GetMapping("/search/keyword")
    public ResponseEntity<SuccessResponseDto<PlaceMapSearchResponse>> searchPlaces(
        @Parameter(description = "검색 키워드", example = "용산공원") @RequestParam String keyword,
        @Parameter(description = "조회할 반려동물 ID (없으면 회색 마커)", example = "1")
        @RequestParam(required = false) Long petId,
        Authentication authentication) {

        String email = authenticatedEmail(authentication);

        log.info("키워드 장소 마커 검색: keyword={}, petId={}, email={}", keyword, petId, email);

        PlaceMapSearchResponse response = placeService.searchByKeyword(keyword, petId, email);

        return ResponseEntity.ok(SuccessResponseDto.success("키워드 장소 목록을 조회했습니다.", response));
    }

    @Operation(summary = "키워드 검색 바텀시트 목록", description = "거리순으로 페이지 처리한 장소 목록을 조회합니다.")
    @GetMapping("/search/keyword/list")
    public ResponseEntity<SuccessResponseDto<PageResponse<PlaceMarkerResponse>>> getKeywordPlaceList(
        @RequestParam String keyword,
        @RequestParam(required = false) Long petId,
        @RequestParam double currMapX,
        @RequestParam double currMapY,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        String email = authenticatedEmail(authentication);
        PageResponse<PlaceMarkerResponse> response = placeService.searchKeywordList(
            keyword, petId, email, currMapX, currMapY, page, size
        );
        return ResponseEntity.ok(SuccessResponseDto.success("키워드 장소 목록을 조회했습니다.", response));
    }

    @Operation(
        summary = "지역별 장소 검색",
        description = "시도 및 시군구 기준으로 해당 장소를 조회합니다."
    )
    @GetMapping("/region/{lDongRegnNm}/{lDongSignguNm}")
    public ResponseEntity<SuccessResponseDto<List<PlaceMarkerResponse>>> searchByRegion(
        @Parameter(description = "시도 이름", example = "서울특별시") @PathVariable String lDongRegnNm,
        @Parameter(description = "시군구 이름", example = "강남구") @PathVariable String lDongSignguNm,
        @Parameter(description = "조회할 반려동물 ID (없으면 회색 마커)", example = "1")
        @RequestParam(required = false) Long petId,
        @Parameter(description = "사용자 현재 경도 좌표", example = "127.1234") @RequestParam double currMapX,
        @Parameter(description = "사용자 현재 위도 좌표", example = "37.1234") @RequestParam double currMapY,
        Authentication authentication) {

        String email = authenticatedEmail(authentication);
        log.info("지역 기반 장소 검색: 시도 이름 = {}, 시군구 이름 = {}, petId = {}, email = {}", lDongRegnNm, lDongSignguNm, petId, email);
        List<PlaceMarkerResponse> response = placeService.searchByRegion(lDongRegnNm, lDongSignguNm, petId, email, currMapX, currMapY);

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
        @Parameter(description = "조회할 반려동물 ID (없으면 회색 마커)", example = "1")
        @RequestParam(required = false) Long petId,
        @Parameter(description = "첫 리뷰 페이지 크기", example = "10")
        @RequestParam(defaultValue = "10") int reviewSize,
        @Parameter(description = "리뷰 정렬 옵션", example = "latest")
        @RequestParam(defaultValue = "latest") String reviewSort,
        Authentication authentication) {

        String email = authenticatedEmail(authentication);

        log.info("장소 상세 조회: placeId={}, petId={}", placeId, petId);

        PlaceDetailResponse response = placeService.getPlaceDetail(
            placeId,
            petId,
            email,
            mapX,
            mapY,
            reviewSize,
            reviewSort
        );

        return ResponseEntity.ok(SuccessResponseDto.success("장소 상세 정보를 조회했습니다.", response));
    }

    private String authenticatedEmail(Authentication authentication) {
        if (authentication == null
            || !authentication.isAuthenticated()
            || authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }
        String email = authentication.getName();
        return email == null || email.isBlank() ? null : email;
    }
}
