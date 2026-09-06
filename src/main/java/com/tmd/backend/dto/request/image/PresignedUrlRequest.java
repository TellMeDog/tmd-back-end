package com.tmd.backend.dto.request.image;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PresignedUrlRequest(
    @NotBlank(message = "파일 이름은 필수입니다.")
    @Size(max = 255, message = "파일 이름은 255자를 초과할 수 없습니다.")
    String filename,

    @NotBlank(message = "Content-Type은 필수입니다.")
    @Size(max = 100, message = "Content-Type은 100자를 초과할 수 없습니다.")
    String contentType,

    @Positive(message = "파일 크기는 0보다 커야 합니다.")
    long fileSize
) {
}
