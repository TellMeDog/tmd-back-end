package com.tmd.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tourapi.category-sync.enabled", havingValue = "true")
public class TourCategorySyncScheduler {
    private final TourCategorySyncService syncService;

    @Scheduled(
        cron = "${tourapi.category-sync.cron:0 30 4 * * SUN}",
        zone = "${tourapi.category-sync.zone:Asia/Seoul}"
    )
    public void synchronize() {
        syncService.synchronize();
    }
}
