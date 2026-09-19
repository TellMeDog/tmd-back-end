package com.tmd.backend.dto.response.place;

import java.io.Serializable;
import java.util.List;

public record PlaceCategoryResponse(
    String code,
    String name,
    int depth,
    List<PlaceCategoryResponse> children
) implements Serializable {
}
