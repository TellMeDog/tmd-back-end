package com.tmd.backend.controller;

import com.tmd.backend.dto.request.image.PresignedUrlRequest;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.image.ImageUploadCompleteResponse;
import com.tmd.backend.dto.response.image.PresignedUrlResponse;
import com.tmd.backend.service.image.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Image", description = "이미지 추가 API")
@RestController
@RequestMapping("/images")
@RequiredArgsConstructor
public class ImageController {
    private final ImageService imageService;

    @Operation(
        summary = "사용자가 이미지 선택",
        description = "사용자가 이미지를 선택할때 호출하는 API입니다."
    )
    @PostMapping("/presigned-url")
    public ResponseEntity<SuccessResponseDto<PresignedUrlResponse>> getPresignedUrl(
        @Valid @RequestBody PresignedUrlRequest request,
        @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "업로드 URL을 발급했습니다.", imageService.initiateUpload(email, request)));
    }

    @Operation(
        summary = "사용자가 이미지 업로드를 확정",
        description = "사용자가 이미지를 선택 후 반려견 등록 혹은 리뷰 작성을 제출할 때 호출하는 API입니다."
    )
    @PostMapping("/uploads/{uploadId}/complete")
    public ResponseEntity<SuccessResponseDto<ImageUploadCompleteResponse>> completeUpload(
        @PathVariable UUID uploadId,
        @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "이미지 업로드를 완료했습니다.", imageService.completeUpload(email, uploadId)));
    }
}
