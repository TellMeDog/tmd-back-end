package com.tmd.backend.service.place.sync;

import com.tmd.backend.external.animalhospital.AnimalHospitalApiClient;
import com.tmd.backend.external.animalhospital.AnimalHospitalItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalHospitalSyncService {
    private final AnimalHospitalApiClient apiClient;
    private final AnimalHospitalSnapshotProcessor snapshotProcessor;

    @SchedulerLock(name = "animalHospitalPlaceSync", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public AnimalHospitalSyncSummary synchronize() {
        long startedAt = System.currentTimeMillis();
        List<AnimalHospitalItem> snapshot = apiClient.getAll();
        AnimalHospitalSyncSummary summary = snapshotProcessor.apply(snapshot);
        log.info("Animal hospital synchronization completed. summary={}, elapsedMs={}",
            summary, System.currentTimeMillis() - startedAt);
        return summary;
    }
}
