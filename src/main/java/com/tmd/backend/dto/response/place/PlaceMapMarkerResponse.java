package com.tmd.backend.dto.response.place;

public record PlaceMapMarkerResponse(
    Long placeId,
    Double mapX,
    Double mapY,
    String markerColor,
    String placeType
) {
}
