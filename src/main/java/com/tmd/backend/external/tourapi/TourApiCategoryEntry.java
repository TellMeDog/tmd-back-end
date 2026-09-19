package com.tmd.backend.external.tourapi;

public record TourApiCategoryEntry(
    String code,
    String name,
    int depth,
    String parentCode,
    int displayOrder
) {
}
