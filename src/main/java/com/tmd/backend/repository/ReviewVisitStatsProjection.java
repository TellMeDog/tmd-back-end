package com.tmd.backend.repository;

import java.time.LocalDateTime;

public interface ReviewVisitStatsProjection {
    Long getEnteredCount();
    Long getMismatchedCount();
    Long getDeniedCount();
    LocalDateTime getLastReportedAt();
}
