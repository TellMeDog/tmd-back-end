package com.tmd.backend.service.place.sync;

public record TourCategorySyncSummary(
    int total,
    int created,
    int updated,
    int deactivated
) {
}
