package com.tmd.backend.service;

import com.tmd.backend.external.TourApiCategoryEntry;
import com.tmd.backend.external.TourApiClient;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TourCategorySyncServiceTest {
    private final TourApiClient client = mock(TourApiClient.class);
    private final TourCategorySnapshotProcessor processor = mock(TourCategorySnapshotProcessor.class);
    private final CacheManager cacheManager = mock(CacheManager.class);
    private final TourCategorySyncService service = new TourCategorySyncService(client, processor, cacheManager);

    @Test
    void appliesCompleteSnapshotAndClearsCategoryCache() {
        List<TourApiCategoryEntry> entries = List.of(
            new TourApiCategoryEntry("FD", "음식", 1, null, 1)
        );
        TourCategorySyncSummary summary = new TourCategorySyncSummary(1, 1, 0, 0);
        Cache cache = mock(Cache.class);
        when(client.getTourCategoryCodes()).thenReturn(entries);
        when(processor.apply(entries)).thenReturn(summary);
        when(cacheManager.getCache("placeCategories")).thenReturn(cache);

        assertThat(service.synchronize()).isEqualTo(summary);

        verify(cache).clear();
    }
}
