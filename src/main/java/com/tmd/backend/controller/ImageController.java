package com.tmd.backend.controller;

import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.image.PresignedUrlResponse;
import com.tmd.backend.service.ImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @GetMapping("/presigned-url")
    public ResponseEntity<SuccessResponseDto<PresignedUrlResponse>> getPresignedUrl(
            @RequestParam String filename) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "업로드 URL이 발급되었습니다.", imageService.generatePresignedUrl(filename)));
    }
}
