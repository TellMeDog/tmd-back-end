package com.tmd.backend.dto.response.image;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PresignedUrlResponse {
    private String presignedUrl; // 프론트엔드가 PUT 요청 보낼 서명된 URL
    private String imageKey;     // DB에 저장할 경로 (ex. images/uuid.jpg)
    private String imageUrl;     // 업로드 후 접근할 공개 URL
}
