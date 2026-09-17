package com.tmd.backend.service;

public record PlaceSyncSummary(
    int apiCount,
    int created,
    int changed,
    int reactivated,
    int deactivated,
    int pendingDetails,
    int detailSucceeded,
    int detailNoData,
    int detailFailed,
    int policiesUpdated,
    int reviewsCreated
) {
}
