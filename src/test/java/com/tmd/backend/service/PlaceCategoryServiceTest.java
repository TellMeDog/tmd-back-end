package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.place.TourCategory;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.PlaceRepository;
import com.tmd.backend.repository.TourCategoryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceCategoryServiceTest {
    private final TourCategoryRepository categoryRepository = mock(TourCategoryRepository.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlaceCategoryService service = new PlaceCategoryService(categoryRepository, placeRepository);

    @Test
    void returnsOnlyCategoriesUsedByActivePlacesAsTree() {
        LocalDateTime now = LocalDateTime.now();
        TourCategory food = TourCategory.create("FD", "음식", 1, null, 1, now);
        TourCategory cafe = TourCategory.create("FD05", "카페", 2, "FD", 1, now);
        TourCategory unused = TourCategory.create("FD01", "한식", 2, "FD", 2, now);
        TourCategory brunch = TourCategory.create("FD050100", "브런치카페", 3, "FD05", 1, now);
        when(categoryRepository.findAllByActiveTrueOrderByDepthAscDisplayOrderAscCodeAsc())
            .thenReturn(List.of(food, cafe, unused, brunch));
        when(placeRepository.findDistinctActiveCategoryPaths())
            .thenReturn(List.<Object[]>of(new Object[]{"FD", "FD05", "FD050100"}));

        var result = service.getCategories();

        assertThat(result).singleElement().satisfies(root -> {
            assertThat(root.code()).isEqualTo("FD");
            assertThat(root.children()).singleElement().satisfies(level2 -> {
                assertThat(level2.code()).isEqualTo("FD05");
                assertThat(level2.children()).singleElement()
                    .satisfies(level3 -> assertThat(level3.code()).isEqualTo("FD050100"));
            });
        });
    }

    @Test
    void rejectsInactiveOrUnknownCategory() {
        when(categoryRepository.findByCodeAndActiveTrue("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getActiveCategory("UNKNOWN"))
            .isInstanceOf(BaseException.class)
            .satisfies(error -> assertThat(((BaseException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CATEGORY));
    }
}
