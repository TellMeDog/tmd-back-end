package com.tmd.backend.external;

public record TourApiCategoryEntry(
    String code,
    String name,
    int depth,
    String parentCode,
    int displayOrder
) {
}
