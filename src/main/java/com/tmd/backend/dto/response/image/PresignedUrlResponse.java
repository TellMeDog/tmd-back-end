package com.tmd.backend.dto.response.image;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class PresignedUrlResponse {
    private String presignedUrl;
    private String imageKey;
    private String imageUrl;
    private Map<String, String> requiredHeaders;
    private long expiresInSeconds;
}
