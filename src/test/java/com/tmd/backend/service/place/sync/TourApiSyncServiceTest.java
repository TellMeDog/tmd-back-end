package com.tmd.backend.service.place.sync;

import com.tmd.backend.common.PetInfoStatus;
import com.tmd.backend.external.tourapi.TourApiClient;
import com.tmd.backend.external.tourapi.TourApiPlaceItem;
import com.tmd.backend.repository.place.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TourApiSyncServiceTest {
    private final TourApiClient tourApiClient = mock(TourApiClient.class);
    private final PlaceSnapshotProcessor snapshotProcessor = mock(PlaceSnapshotProcessor.class);
    private final PlaceDetailSyncProcessor detailProcessor = mock(PlaceDetailSyncProcessor.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final TourApiSyncService service = new TourApiSyncService(
        tourApiClient,
        snapshotProcessor,
        detailProcessor,
        placeRepository
    );

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "detailLimit", 10);
        when(tourApiClient.getTourSyncList()).thenReturn(List.of(new TourApiPlaceItem()));
        when(snapshotProcessor.apply(anyList()))
            .thenReturn(new PlaceSnapshotProcessor.SnapshotResult(1, 2, 3, 4));
    }

    @Test
    void 개별_실패는_집계하고_다음_장소를_계속_처리한다() {
        when(placeRepository.findPendingPetInfoSyncIds()).thenReturn(List.of(1L, 2L, 3L));
        when(detailProcessor.process(1L))
            .thenReturn(new PlaceDetailSyncProcessor.DetailResult(PetInfoStatus.SUCCESS, true, false));
        when(detailProcessor.process(2L)).thenThrow(new IllegalStateException("temporary failure"));
        when(detailProcessor.process(3L))
            .thenReturn(new PlaceDetailSyncProcessor.DetailResult(PetInfoStatus.NO_DATA, true, true));

        PlaceSyncSummary result = service.synchronize();

        assertThat(result.apiCount()).isEqualTo(1);
        assertThat(result.created()).isEqualTo(1);
        assertThat(result.changed()).isEqualTo(2);
        assertThat(result.reactivated()).isEqualTo(3);
        assertThat(result.deactivated()).isEqualTo(4);
        assertThat(result.pendingDetails()).isEqualTo(3);
        assertThat(result.detailSucceeded()).isEqualTo(1);
        assertThat(result.detailNoData()).isEqualTo(1);
        assertThat(result.detailFailed()).isEqualTo(1);
        assertThat(result.policiesUpdated()).isEqualTo(2);
        assertThat(result.reviewsCreated()).isEqualTo(1);
        verify(detailProcessor).process(3L);
    }

    @Test
    void 호출_한도_초과가_발생하면_남은_장소를_처리하지_않는다() {
        when(placeRepository.findPendingPetInfoSyncIds()).thenReturn(List.of(1L, 2L, 3L));
        when(detailProcessor.process(1L))
            .thenReturn(new PlaceDetailSyncProcessor.DetailResult(PetInfoStatus.SUCCESS, true, false));
        when(detailProcessor.process(2L)).thenThrow(tooManyRequests());

        PlaceSyncSummary result = service.synchronize();

        assertThat(result.detailSucceeded()).isEqualTo(1);
        assertThat(result.detailFailed()).isEqualTo(1);
        verify(detailProcessor, never()).process(3L);
    }

    @Test
    void detailLimit까지만_처리한다() {
        ReflectionTestUtils.setField(service, "detailLimit", 2);
        when(placeRepository.findPendingPetInfoSyncIds()).thenReturn(List.of(1L, 2L, 3L));
        when(detailProcessor.process(anyLong()))
            .thenReturn(new PlaceDetailSyncProcessor.DetailResult(PetInfoStatus.SUCCESS, true, false));

        PlaceSyncSummary result = service.synchronize();

        assertThat(result.pendingDetails()).isEqualTo(3);
        assertThat(result.detailSucceeded()).isEqualTo(2);
        verify(detailProcessor, never()).process(3L);
    }

    private HttpClientErrorException.TooManyRequests tooManyRequests() {
        return (HttpClientErrorException.TooManyRequests) HttpClientErrorException.create(
            HttpStatus.TOO_MANY_REQUESTS,
            "Too Many Requests",
            HttpHeaders.EMPTY,
            new byte[0],
            StandardCharsets.UTF_8
        );
    }
}
