package com.tmd.backend.dto.response.place;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PlaceSummaryResponse {
    private Long placeId;
    private String title;
    private String markerColor;
    private String conditionSummary;
    private boolean isFavorite;

    @Builder
    public PlaceSummaryResponse(Long placeId, String title, String markerColor, String conditionSummary, boolean isFavorite) {
        this.placeId = placeId;
        this.title = title;
        this.markerColor = markerColor;
        this.conditionSummary = conditionSummary;
        this.isFavorite = isFavorite;
    }
}
