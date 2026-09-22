package com.tmd.backend.service.place;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.common.PlaceSearchCategory;
import com.tmd.backend.domain.place.TourCategory;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.place.PlaceRepository;
import com.tmd.backend.repository.place.TourCategoryRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

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
    void resolvesKoreanSearchCategoryAfterTrimming() {
        PlaceSearchCategory result = service.getSearchCategory("  축제  ");

        assertThat(result).isEqualTo(PlaceSearchCategory.FESTIVAL);
        assertThat(result.getDepth()).isEqualTo(2);
        assertThat(result.getQueryCode()).isEqualTo("EV01");
    }

    @Test
    void returnsSearchCategoryMenuInDisplayOrder() {
        assertThat(service.getSearchCategories())
            .extracting(response -> response.category())
            .containsExactly(
                "전체", "숙박", "축제", "공연", "행사", "체험관광", "식당카페",
                "역사관광", "레저스포츠", "자연관광", "쇼핑", "문화관광", "동물병원"
            );
    }

    @Test
    void mapsEverySearchCategoryToExpectedDepthAndCode() {
        assertThat(PlaceSearchCategory.values())
            .extracting(category -> String.join(":",
                category.getDisplayName(),
                String.valueOf(category.getDepth()),
                String.valueOf(category.getQueryCode())
            ))
            .containsExactly(
                "전체:0:null", "숙박:1:AC", "축제:2:EV01", "공연:2:EV02", "행사:2:EV03",
                "체험관광:1:EX", "식당카페:1:FD", "역사관광:1:HS", "레저스포츠:1:LS",
                "자연관광:1:NA", "쇼핑:1:SH", "문화관광:1:VE", "동물병원:1:TMDHOSP"
            );
    }

    @Test
    void rejectsUnknownKoreanSearchCategory() {
        assertThatThrownBy(() -> service.getSearchCategory("여행"))
            .isInstanceOf(BaseException.class)
            .satisfies(error -> assertThat(((BaseException) error).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CATEGORY));
    }
}
