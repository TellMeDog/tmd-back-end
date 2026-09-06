package com.tmd.backend.controller;

import com.tmd.backend.dto.request.image.PresignedUrlRequest;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.image.PresignedUrlResponse;
import com.tmd.backend.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/presigned-url")
    public ResponseEntity<SuccessResponseDto<PresignedUrlResponse>> getPresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "업로드 URL이 발급되었습니다.", imageService.generatePresignedUrl(request)));
    }
}
