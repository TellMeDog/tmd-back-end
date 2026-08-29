package com.tmd.backend.dto.response.place;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PlaceMarkerResponse {
    private Long placeId;
    private double mapX;
    private double mapY;
    private String markerColor;

    @Builder
    public PlaceMarkerResponse(Long placeId, double mapX, double mapY, String markerColor) {
        this.placeId = placeId;
        this.mapX = mapX;
        this.mapY = mapY;
        this.markerColor = markerColor;
    }
}
