package com.tmd.backend.service.place.sync;

import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.TourCategory;
import com.tmd.backend.external.animalhospital.AnimalHospitalCoordinateConverter;
import com.tmd.backend.external.animalhospital.AnimalHospitalItem;
import com.tmd.backend.repository.place.PlaceRepository;
import com.tmd.backend.repository.place.TourCategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalHospitalSnapshotProcessor {
    public static final String CATEGORY_CODE = "TMDHOSP";
    private static final String CONTENT_ID_PREFIX = "MOIS-HOSPITAL:";

    private final PlaceRepository placeRepository;
    private final TourCategoryRepository categoryRepository;
    private final AnimalHospitalCoordinateConverter coordinateConverter;
    private final PlaceRegionCodeResolver regionCodeResolver;

    @Transactional
    @CacheEvict(value = "placeCategories", allEntries = true)
    public AnimalHospitalSyncSummary apply(List<AnimalHospitalItem> items) {
        validateSnapshot(items);
        ensureCategory();

        Map<String, Place> existing = new HashMap<>();
        placeRepository.findAllAnimalHospitalsForSync()
            .forEach(place -> existing.put(place.getContentId(), place));
        Set<String> receivedIds = new HashSet<>();
        int eligible = 0, created = 0, updated = 0, deactivated = 0, skipped = 0;

        for (AnimalHospitalItem item : items) {
            String managementNumber = trimToNull(item.managementNumber());
            if (managementNumber == null) {
                skipped++;
                continue;
            }
            String contentId = CONTENT_ID_PREFIX + managementNumber;
            receivedIds.add(contentId);
            Place place = existing.get(contentId);

            HospitalValues values = item.isOpen() ? toValues(item) : null;
            if (values == null) {
                skipped++;
                if (place != null && place.isActive()) {
                    place.deactivate();
                    deactivated++;
                }
                continue;
            }
            eligible++;
            if (place == null) {
                placeRepository.save(Place.createAnimalHospital(
                    contentId, values.zipCode(), values.address(), values.title(),
                    values.longitude(), values.latitude(), values.modifiedTime(), values.telephone(),
                    values.businessStatus(), values.regionCode(), values.districtCode(), CATEGORY_CODE
                ));
                created++;
            } else {
                place.updateFromAnimalHospital(
                    values.zipCode(), values.address(), values.title(), values.longitude(), values.latitude(),
                    values.modifiedTime(), values.telephone(), values.businessStatus(),
                    values.regionCode(), values.districtCode(), CATEGORY_CODE
                );
                updated++;
            }
        }

        for (Place place : existing.values()) {
            if (!receivedIds.contains(place.getContentId()) && place.isActive()) {
                place.deactivate();
                deactivated++;
            }
        }
        placeRepository.flush();
        return new AnimalHospitalSyncSummary(items.size(), eligible, created, updated, deactivated, skipped);
    }

    private void validateSnapshot(List<AnimalHospitalItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalStateException("Animal hospital snapshot is empty");
        }
        Set<String> managementNumbers = new HashSet<>();
        for (AnimalHospitalItem item : items) {
            if (item == null) {
                throw new IllegalStateException("Animal hospital snapshot contains a null item");
            }
            String managementNumber = trimToNull(item.managementNumber());
            if (managementNumber != null && !managementNumbers.add(managementNumber)) {
                throw new IllegalStateException("Animal hospital snapshot contains duplicate MNG_NO: " + managementNumber);
            }
        }
    }

    private HospitalValues toValues(AnimalHospitalItem item) {
        String title = trimToNull(item.name());
        if (title == null) {
            return null;
        }
        AnimalHospitalCoordinateConverter.Coordinates coordinates;
        try {
            coordinates = coordinateConverter.convert(item.coordinateX(), item.coordinateY());
        } catch (IllegalArgumentException exception) {
            log.debug("Skipping animal hospital with unusable coordinates. mngNo={}", item.managementNumber());
            return null;
        }

        String address = firstNonBlank(item.roadAddress(), item.lotNumberAddress());
        PlaceRegionCodeResolver.RegionCodes regionCodes = regionCodeResolver.resolve(address);
        return new HospitalValues(
            firstNonBlank(item.roadZipCode(), item.locationZipCode()),
            address,
            title,
            coordinates.longitude(),
            coordinates.latitude(),
            firstNonBlank(item.lastModifiedAt(), item.dataUpdatedAt()),
            trimToNull(item.telephone()),
            firstNonBlank(item.statusName(), item.detailStatusName()),
            regionCodes.regionCode(),
            regionCodes.districtCode()
        );
    }

    private void ensureCategory() {
        TourCategory category = categoryRepository.findById(CATEGORY_CODE).orElse(null);
        if (category == null) {
            categoryRepository.save(TourCategory.createSystem(
                CATEGORY_CODE, "동물병원", Integer.MAX_VALUE, LocalDateTime.now()
            ));
        }
    }

    private String firstNonBlank(String first, String second) {
        String value = trimToNull(first);
        return value != null ? value : trimToNull(second);
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record HospitalValues(
        String zipCode,
        String address,
        String title,
        double longitude,
        double latitude,
        String modifiedTime,
        String telephone,
        String businessStatus,
        String regionCode,
        String districtCode
    ) {}
}
