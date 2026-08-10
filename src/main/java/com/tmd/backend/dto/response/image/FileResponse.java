package com.tmd.backend.dto.response.image;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FileResponse {
    private String imageUrl;

    @Builder
    public FileResponse(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
