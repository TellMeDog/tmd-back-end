package com.tmd.backend.service.place.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "tourapi.sync.enabled", havingValue = "true")
public class TourApiSyncScheduler {
    private final TourApiSyncService syncService;

    @Scheduled(cron = "${tourapi.sync.cron:0 0 5 * * *}", zone = "${tourapi.sync.zone:Asia/Seoul}")
    public void synchronize() {
        syncService.synchronize();
    }
}
