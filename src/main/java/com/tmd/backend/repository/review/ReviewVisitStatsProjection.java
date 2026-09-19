package com.tmd.backend.repository.review;

import java.time.LocalDateTime;

public interface ReviewVisitStatsProjection {
    Long getEnteredCount();
    Long getMismatchedCount();
    Long getDeniedCount();
    LocalDateTime getLastReportedAt();
}
