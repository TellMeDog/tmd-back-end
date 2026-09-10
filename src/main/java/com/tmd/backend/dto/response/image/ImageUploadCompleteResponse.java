package com.tmd.backend.dto.response.image;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class ImageUploadCompleteResponse {
    private UUID uploadId;
    private String imageUrl;
}
