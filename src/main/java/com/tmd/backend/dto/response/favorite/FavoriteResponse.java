package com.tmd.backend.dto.response.favorite;

import lombok.Builder;
import lombok.Getter;

@Getter
public class FavoriteResponse {
    private Long placeId;
    private String contentId;
    private String thumbnailUrl;
    private String title;
    private String addr;
    private String markerColor;

    @Builder
    public FavoriteResponse(Long placeId, String contentId, String thumbnailUrl, String title, String addr, String markerColor) {
        this.placeId = placeId;
        this.contentId = contentId;
        this.thumbnailUrl=thumbnailUrl;
        this.title = title;
        this.addr = addr;
        this.markerColor = markerColor;
    }
}
