package com.tmd.backend.service.place.sync;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "animal-hospital.sync.enabled", havingValue = "true")
public class AnimalHospitalSyncScheduler {
    private final AnimalHospitalSyncService syncService;

    @Scheduled(
        cron = "${animal-hospital.sync.cron:0 30 5 * * *}",
        zone = "${animal-hospital.sync.zone:Asia/Seoul}"
    )
    public void synchronize() {
        syncService.synchronize();
    }
}
