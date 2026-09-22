package com.tmd.backend.dto.response.place;

import java.io.Serializable;

public record PlaceSearchCategoryResponse(
    String category
) implements Serializable {
}
