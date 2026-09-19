package com.tmd.backend.service;

import com.tmd.backend.domain.place.Place;
import com.tmd.backend.external.TourApiPlaceItem;
import com.tmd.backend.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class PlaceSnapshotProcessor {
    private final PlaceRepository placeRepository;

    @Transactional
    @CacheEvict(value = "placeCategories", allEntries = true)
    public SnapshotResult apply(List<TourApiPlaceItem> items) {
        Map<String, Place> existing = new HashMap<>();
        placeRepository.findAllForSync().forEach(place -> existing.put(place.getContentId(), place));
        Set<String> receivedIds = new HashSet<>();
        int created = 0, changed = 0, reactivated = 0, deactivated = 0;

        for (TourApiPlaceItem item : items) {
            if (item.getContentid() == null || !receivedIds.add(item.getContentid())) {
                throw new IllegalStateException("TourAPI Sync에 contentId 누락 또는 중복이 있습니다: " + item.getContentid());
            }
            Place place = existing.get(item.getContentid());
            if (place == null) {
                place = Place.from(item);
                place.updateFrom(item);
                placeRepository.save(place);
                created++;
                continue;
            }

            String previousModifiedTime = place.getModifiedTime();
            boolean wasActive = place.isActive();
            place.updateFrom(item);
            place.initializePetInfoSyncVersionIfUnchanged(previousModifiedTime);
            if (!Objects.equals(previousModifiedTime, item.getModifiedtime())) changed++;
            if (!wasActive && place.isActive()) {
                reactivated++;
                place.requestPetInfoSync();
            }
            if (wasActive && !place.isActive()) deactivated++;
        }

        for (Place place : existing.values()) {
            if (!receivedIds.contains(place.getContentId()) && place.isActive()) {
                place.deactivate();
                deactivated++;
            }
        }
        placeRepository.flush();
        return new SnapshotResult(created, changed, reactivated, deactivated);
    }

    public record SnapshotResult(int created, int changed, int reactivated, int deactivated) {}
}
