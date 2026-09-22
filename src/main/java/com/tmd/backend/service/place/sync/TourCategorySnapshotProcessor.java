package com.tmd.backend.service.place.sync;

import com.tmd.backend.domain.place.TourCategory;
import com.tmd.backend.external.tourapi.TourApiCategoryEntry;
import com.tmd.backend.repository.place.TourCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TourCategorySnapshotProcessor {
    private final TourCategoryRepository categoryRepository;

    @Transactional
    public TourCategorySyncSummary apply(List<TourApiCategoryEntry> entries) {
        validate(entries);

        LocalDateTime syncedAt = LocalDateTime.now();
        Map<String, TourCategory> existing = new HashMap<>();
        categoryRepository.findAll().forEach(category -> existing.put(category.getCode(), category));
        Set<String> receivedCodes = new HashSet<>();
        int created = 0;
        int updated = 0;

        for (TourApiCategoryEntry entry : entries) {
            receivedCodes.add(entry.code());
            TourCategory category = existing.get(entry.code());
            if (category == null) {
                categoryRepository.save(TourCategory.create(
                    entry.code(), entry.name(), entry.depth(), entry.parentCode(), entry.displayOrder(), syncedAt
                ));
                created++;
            } else {
                category.update(
                    entry.name(), entry.depth(), entry.parentCode(), entry.displayOrder(), syncedAt
                );
                updated++;
            }
        }

        int deactivated = 0;
        for (TourCategory category : existing.values()) {
            if (category.isTourApiManaged()
                && !receivedCodes.contains(category.getCode())
                && category.isActive()) {
                category.deactivate(syncedAt);
                deactivated++;
            }
        }
        categoryRepository.flush();
        return new TourCategorySyncSummary(entries.size(), created, updated, deactivated);
    }

    private void validate(List<TourApiCategoryEntry> entries) {
        if (entries.isEmpty()) {
            throw new IllegalStateException("TourAPI category response is empty");
        }
        Map<String, TourApiCategoryEntry> byCode = new HashMap<>();
        for (TourApiCategoryEntry entry : entries) {
            if (entry.code() == null || entry.code().isBlank()
                || entry.name() == null || entry.name().isBlank()
                || entry.depth() < 1 || entry.depth() > 3
                || byCode.put(entry.code(), entry) != null) {
                throw new IllegalStateException("TourAPI category response contains invalid or duplicate data");
            }
        }
        for (TourApiCategoryEntry entry : entries) {
            if (entry.depth() == 1 && entry.parentCode() != null) {
                throw new IllegalStateException("Top-level category cannot have a parent: " + entry.code());
            }
            if (entry.depth() > 1) {
                TourApiCategoryEntry parent = byCode.get(entry.parentCode());
                if (parent == null || parent.depth() != entry.depth() - 1) {
                    throw new IllegalStateException("Category parent is missing or invalid: " + entry.code());
                }
            }
        }
    }
}
