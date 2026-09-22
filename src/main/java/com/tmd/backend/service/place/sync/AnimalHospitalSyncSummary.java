package com.tmd.backend.service.place.sync;

public record AnimalHospitalSyncSummary(
    int received,
    int eligible,
    int created,
    int updated,
    int deactivated,
    int skipped
) {}
