package com.tmd.backend.controller;

import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.service.SeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class SeedController {
    private final SeedService seedService;

    @PostMapping("/seed/places")
    public ResponseEntity<SuccessResponseDto<Void>> setFirstSeed(){
        log.info("시딩 요청");
        seedService.seedAllPlaces();
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("데이터 초기세팅이 완료되었습니다."));
    }
}
