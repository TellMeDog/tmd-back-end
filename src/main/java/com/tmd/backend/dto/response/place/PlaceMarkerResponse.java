package com.tmd.backend.dto.response.place;

import lombok.Builder;
import lombok.Getter;

@Getter
public class PlaceMarkerResponse {
    private Long placeId;
    private String title;
    private String firstImage;
    private Double mapX;
    private Double mapY;
    private long distance;
    private boolean isFavorite;
    private String markerColor;
    private double averageRating;
    private String placeType;

    @Builder
    public PlaceMarkerResponse(Long placeId, String title, String firstImage, Double mapX, Double mapY,
                               long distance, boolean isFavorite, String markerColor,
                               double averageRating, String placeType) {
        this.placeId = placeId;
        this.title = title;
        this.firstImage = firstImage;
        this.mapX = mapX;
        this.mapY = mapY;
        this.distance = distance;
        this.isFavorite = isFavorite;
        this.markerColor = markerColor;
        this.averageRating=averageRating;
        this.placeType = placeType;
    }
}
