package com.tmd.backend.service;

import com.tmd.backend.common.PetInfoStatus;
import com.tmd.backend.external.TourApiClient;
import com.tmd.backend.external.TourApiPlaceItem;
import com.tmd.backend.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TourApiSyncService {
    private final TourApiClient tourApiClient;
    private final PlaceSnapshotProcessor snapshotProcessor;
    private final PlaceDetailSyncProcessor detailProcessor;
    private final PlaceRepository placeRepository;

    @Value("${tourapi.sync.detail-limit:500}")
    private int detailLimit;

    @SchedulerLock(name = "tourApiPlaceSync", lockAtMostFor = "PT2H", lockAtLeastFor = "PT1M")
    public PlaceSyncSummary synchronize() {
        long startedAt = System.currentTimeMillis();
        List<TourApiPlaceItem> snapshot = tourApiClient.getTourSyncList();
        if (snapshot.isEmpty()) throw new IllegalStateException("TourAPI Sync 전체 응답이 비어 있습니다.");

        PlaceSnapshotProcessor.SnapshotResult snapshotResult = snapshotProcessor.apply(snapshot);
        List<Long> pending = placeRepository.findPendingPetInfoSyncIds();
        int success = 0, noData = 0, failed = 0, policyUpdated = 0, reviews = 0;

        for (Long placeId : pending.stream().limit(detailLimit).toList()) {
            try {
                PlaceDetailSyncProcessor.DetailResult result = detailProcessor.process(placeId);
                if (result.status() == PetInfoStatus.SUCCESS) success++;
                if (result.status() == PetInfoStatus.NO_DATA) noData++;
                if (result.policyUpdated()) policyUpdated++;
                if (result.reviewCreated()) reviews++;
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("TourAPI 호출 한도 초과로 상세 동기화를 중단합니다. placeId={}", placeId);
                failed++;
                break;
            } catch (RuntimeException e) {
                failed++;
                log.error("장소 상세 동기화 실패. 다음 실행에서 재시도합니다. placeId={}", placeId, e);
            }
        }

        PlaceSyncSummary summary = new PlaceSyncSummary(snapshot.size(), snapshotResult.created(), snapshotResult.changed(),
            snapshotResult.reactivated(), snapshotResult.deactivated(), pending.size(), success, noData, failed,
            policyUpdated, reviews);
        log.info("TourAPI 동기화 완료. summary={}, elapsedMs={}", summary, System.currentTimeMillis() - startedAt);
        return summary;
    }
}
