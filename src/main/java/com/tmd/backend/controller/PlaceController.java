package com.tmd.backend.controller;

import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.place.PlaceDetailResponse;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.dto.response.place.PlaceSummaryResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/places")
public class PlaceController {

    @GetMapping
    public ResponseEntity<SuccessResponseDto<List<PlaceMarkerResponse>>> getPlaces(
        @RequestParam double swLat, @RequestParam double swLng,
        @RequestParam double neLat, @RequestParam double neLng,
        @RequestParam Long petId) {

        log.info("장소 목록 조회: bounds=({},{})~({},{}), petId={}", swLat, swLng, neLat, neLng, petId);

        List<PlaceMarkerResponse> dummy = List.of(
            PlaceMarkerResponse.builder()
                .placeId(12L).contentId("2874523").title("OO 애견카페")
                .mapX(127.123456).mapY(37.123456).markerColor("GREEN")
                .build()
        );

        return ResponseEntity.ok(SuccessResponseDto.success("장소 목록을 조회했습니다.", dummy));
    }

    @GetMapping("/search")
    public ResponseEntity<SuccessResponseDto<List<PlaceMarkerResponse>>> searchPlaces(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String category,
        @RequestParam double swLat, @RequestParam double swLng,
        @RequestParam double neLat, @RequestParam double neLng,
        @RequestParam Long petId) {

        log.info("장소 검색: keyword={}, category={}, petId={}", keyword, category, petId);
        // TODO: 실제로는 keyword/category 동시 입력 시 INVALID_SEARCH_REQUEST 예외 처리 필요

        List<PlaceMarkerResponse> dummy = List.of(
            PlaceMarkerResponse.builder()
                .placeId(13L).contentId("2874999").title("OO 애견식당")
                .mapX(127.111).mapY(37.111).markerColor("ORANGE")
                .build()
        );

        return ResponseEntity.ok(SuccessResponseDto.success("장소 목록을 조회했습니다.", dummy));
    }

    @GetMapping("/{placeId}/summary")
    public ResponseEntity<SuccessResponseDto<PlaceSummaryResponse>> getBanner(
        @PathVariable Long placeId, @RequestParam Long petId) {

        log.info("장소 배너 조회: placeId={}, petId={}", placeId, petId);

        PlaceSummaryResponse response = PlaceSummaryResponse.builder()
            .placeId(placeId)
            .title("OO 애견카페")
            .markerColor("ORANGE")
            .conditionSummary("대형견은 입마개 착용 시 입장 가능")
            .isFavorite(false)
            .build();

        return ResponseEntity.ok(SuccessResponseDto.success("장소 정보를 조회했습니다.", response));
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<PlaceDetailResponse>> getPlaceDetail(
        @PathVariable Long placeId, @RequestParam Long petId) {

        log.info("장소 상세 조회: placeId={}, petId={}", placeId, petId);

        PlaceDetailResponse response = PlaceDetailResponse.builder()
            .placeId(placeId)
            .contentId("2874523")
            .title("OO 애견카페")
            .addr("서울 강남구 ...")
            .mapX(127.123456).mapY(37.123456)
            .markerColor("ORANGE")
            .checklist(PlaceDetailResponse.Checklist.builder()
                .leashRequired(true).muzzleRequired(false).wasteBagRequired(true)
                .build())
            .petPolicyRawText(PlaceDetailResponse.PetPolicyRawText.builder()
                .acmpyTypeCd("일부구역 동반가능")
                .acmpyNeedMtr("목줄 착용")
                .etcAcmpyInfo("- 배변봉투 지참 및 배변처리 필수")
                .build())
            .isFavorite(false)
            .averageRating(4.2)
            .visitStats(PlaceDetailResponse.VisitStats.builder()
                .enteredCount(100).mismatchedCount(37).deniedCount(1)
                .lastReportedAt("2026-08-06T15:20:00")
                .build())
            .build();

        return ResponseEntity.ok(SuccessResponseDto.success("장소 상세 정보를 조회했습니다.", response));
    }
}
