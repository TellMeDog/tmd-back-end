package com.tmd.backend.dto.response.place;

import java.util.List;

public record PlaceMapSearchResponse(
    long totalCount,
    List<PlaceMapMarkerResponse> markers
) {
    public PlaceMapSearchResponse {
        markers = List.copyOf(markers);
    }

    public static PlaceMapSearchResponse from(List<PlaceMapMarkerResponse> markers) {
        return new PlaceMapSearchResponse(markers.size(), markers);
    }
}
