package com.tmd.backend.controller;

import com.tmd.backend.dto.request.image.PresignedUrlRequest;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.image.ImageUploadCompleteResponse;
import com.tmd.backend.dto.response.image.PresignedUrlResponse;
import com.tmd.backend.service.ImageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;

    @PostMapping("/presigned-url")
    public ResponseEntity<SuccessResponseDto<PresignedUrlResponse>> getPresignedUrl(
        @Valid @RequestBody PresignedUrlRequest request,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "업로드 URL을 발급했습니다.", imageService.initiateUpload(email, request)));
    }

    @PostMapping("/uploads/{uploadId}/complete")
    public ResponseEntity<SuccessResponseDto<ImageUploadCompleteResponse>> completeUpload(
        @PathVariable UUID uploadId,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "이미지 업로드를 완료했습니다.", imageService.completeUpload(email, uploadId)));
    }
}
