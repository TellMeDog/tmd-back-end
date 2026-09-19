package com.tmd.backend.service.place;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.place.TourCategory;
import com.tmd.backend.dto.response.place.PlaceCategoryResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.place.PlaceRepository;
import com.tmd.backend.repository.place.TourCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceCategoryService {
    private final TourCategoryRepository categoryRepository;
    private final PlaceRepository placeRepository;

    public TourCategory getActiveCategory(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BaseException(ErrorCode.INVALID_CATEGORY);
        }
        return categoryRepository.findByCodeAndActiveTrue(code.trim())
            .orElseThrow(() -> new BaseException(ErrorCode.INVALID_CATEGORY));
    }

    @Cacheable(value = "placeCategories", key = "'tree'")
    public List<PlaceCategoryResponse> getCategories() {
        Set<String> usedCodes = getUsedCategoryCodes();
        List<TourCategory> categories = categoryRepository
            .findAllByActiveTrueOrderByDepthAscDisplayOrderAscCodeAsc()
            .stream()
            .filter(category -> usedCodes.contains(category.getCode()))
            .toList();

        Map<String, List<TourCategory>> childrenByParent = new HashMap<>();
        for (TourCategory category : categories) {
            childrenByParent.computeIfAbsent(category.getParentCode(), ignored -> new java.util.ArrayList<>())
                .add(category);
        }
        return childrenByParent.getOrDefault(null, List.of()).stream()
            .map(category -> toResponse(category, childrenByParent))
            .toList();
    }

    private Set<String> getUsedCategoryCodes() {
        Set<String> codes = new HashSet<>();
        for (Object[] path : placeRepository.findDistinctActiveCategoryPaths()) {
            for (Object value : path) {
                if (value instanceof String code && StringUtils.hasText(code)) {
                    codes.add(code);
                }
            }
        }
        return codes;
    }

    private PlaceCategoryResponse toResponse(
        TourCategory category,
        Map<String, List<TourCategory>> childrenByParent
    ) {
        List<PlaceCategoryResponse> children = childrenByParent
            .getOrDefault(category.getCode(), List.of())
            .stream()
            .map(child -> toResponse(child, childrenByParent))
            .toList();
        return new PlaceCategoryResponse(
            category.getCode(),
            category.getName(),
            category.getDepth(),
            children
        );
    }
}
