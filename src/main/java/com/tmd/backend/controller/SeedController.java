package com.tmd.backend.controller;

import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.service.SeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class SeedController {
    private final SeedService seedService;

    // Step 1: TourAPI petTourSyncList2 → Place 테이블 적재
    @PostMapping("/seed/places")
    public ResponseEntity<SuccessResponseDto<Void>> seedPlaces() {
        log.info("장소 시딩 요청");
        seedService.seedAllPlaces();
        return ResponseEntity.accepted()
            .body(SuccessResponseDto.successWithoutData("장소 시딩 시작 (백그라운드 실행, 로그 확인)"));
    }

    // Step 2: detailPetTour2 → PlacePetInfo 테이블 적재
    @PostMapping("/seed/pet-policies")
    public ResponseEntity<SuccessResponseDto<Void>> fetchPetPolicies() {
        log.info("반려동물 정책 원본 수집 요청");
        seedService.fetchPetPolicies();
        return ResponseEntity.accepted()
            .body(SuccessResponseDto.successWithoutData("정책 원본 수집 시작 (백그라운드 실행, 로그 확인)"));
    }

    // Step 3: PlacePetInfo → DeepSeek 분석 → PlacePetPolicy 테이블 적재
    // 요청 한도 초과 시 자동 중단, 재호출 시 이어서 처리
    @PostMapping("/seed/analyze")
    public ResponseEntity<SuccessResponseDto<Void>> analyzePolicies() {
        log.info("정책 분석 요청");
        seedService.analyzePetPolicies();
        return ResponseEntity.accepted()
            .body(SuccessResponseDto.successWithoutData("정책 분석 시작 (백그라운드 실행, 로그 확인)"));
    }
}
