package com.tmd.backend.dto.response.image;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;
import java.util.UUID;

@Getter
@Builder
public class PresignedUrlResponse {
    private UUID uploadId;
    private String presignedUrl;
    private Map<String, String> requiredHeaders;
    private long expiresInSeconds;
}
