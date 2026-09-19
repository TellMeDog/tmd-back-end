package com.tmd.backend.service;

public record TourCategorySyncSummary(
    int total,
    int created,
    int updated,
    int deactivated
) {
}
