package com.tmd.backend.service.place.sync;

import com.tmd.backend.external.tourapi.TourApiCategoryEntry;
import com.tmd.backend.external.tourapi.TourApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourCategorySyncService {
    private final TourApiClient tourApiClient;
    private final TourCategorySnapshotProcessor snapshotProcessor;
    private final CacheManager cacheManager;

    @SchedulerLock(name = "tourApiCategorySync", lockAtMostFor = "PT30M", lockAtLeastFor = "PT1M")
    public TourCategorySyncSummary synchronize() {
        List<TourApiCategoryEntry> entries = tourApiClient.getTourCategoryCodes();
        TourCategorySyncSummary summary = snapshotProcessor.apply(entries);
        Cache cache = cacheManager.getCache("placeCategories");
        if (cache != null) cache.clear();
        log.info("TourAPI category synchronization completed. summary={}", summary);
        return summary;
    }
}
