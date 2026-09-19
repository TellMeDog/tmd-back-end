package com.tmd.backend.service.place.sync;

import com.tmd.backend.domain.place.TourCategory;
import com.tmd.backend.external.tourapi.TourApiCategoryEntry;
import com.tmd.backend.repository.place.TourCategoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TourCategorySnapshotProcessorTest {
    private final TourCategoryRepository repository = mock(TourCategoryRepository.class);
    private final TourCategorySnapshotProcessor processor = new TourCategorySnapshotProcessor(repository);

    @Test
    void upsertsSnapshotAndDeactivatesMissingCategory() {
        TourCategory existing = TourCategory.create("FD", "기존 음식", 1, null, 1, LocalDateTime.now());
        TourCategory missing = TourCategory.create("SH", "쇼핑", 1, null, 2, LocalDateTime.now());
        when(repository.findAll()).thenReturn(List.of(existing, missing));
        List<TourApiCategoryEntry> entries = List.of(
            new TourApiCategoryEntry("FD", "음식", 1, null, 1),
            new TourApiCategoryEntry("FD05", "카페", 2, "FD", 1)
        );

        TourCategorySyncSummary result = processor.apply(entries);

        assertThat(result).isEqualTo(new TourCategorySyncSummary(2, 1, 1, 1));
        assertThat(existing.getName()).isEqualTo("음식");
        assertThat(missing.isActive()).isFalse();
        ArgumentCaptor<TourCategory> captor = ArgumentCaptor.forClass(TourCategory.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("FD05");
        verify(repository).flush();
    }

    @Test
    void invalidSnapshotDoesNotReadOrChangeDatabase() {
        List<TourApiCategoryEntry> entries = List.of(
            new TourApiCategoryEntry("FD05", "카페", 2, "FD", 1)
        );

        assertThatThrownBy(() -> processor.apply(entries))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("parent");
        verifyNoInteractions(repository);
    }
}
