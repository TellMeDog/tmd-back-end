package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.common.MarkerColor;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.TourCategory;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlaceCategorySearchTest {

    @Mock PlaceRepository placeRepository;
    @Mock PetRepository petRepository;
    @Mock MarkerColorService markerColorService;
    @Mock ReviewService reviewService;
    @Mock FavoriteService favoriteService;
    @Mock PlaceCategoryService placeCategoryService;

    @InjectMocks PlaceService placeService;

    @Test
    void searchesByOfficialCategoryCode() {
        TourCategory category = mock(TourCategory.class);
        Place place = place(1L, 127.01, 37.01);
        given(category.getDepth()).willReturn(2);
        given(category.getCode()).willReturn("FD05");
        given(placeCategoryService.getActiveCategory("FD05")).willReturn(category);
        given(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, 2, "FD05"
        )).willReturn(List.of(place));
        given(reviewService.getAverageRatings(List.of(1L))).willReturn(Map.of(1L, 4.5));
        given(markerColorService.calculateMarkerColor(null, null)).willReturn(MarkerColor.GREY);

        List<PlaceMarkerResponse> result = placeService.searchByCategory(
            "FD05", null, null,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0
        );

        assertThat(result).singleElement().satisfies(marker -> {
            assertThat(marker.getPlaceId()).isEqualTo(1L);
            assertThat(marker.getAverageRating()).isEqualTo(4.5);
        });
    }

    @Test
    void omittedCategoryReturnsEveryPlaceWithinBounds() {
        Place place = place(1L, 127.01, 37.01);
        given(placeRepository.findPlacesWithinBounds(37.0, 127.0, 38.0, 128.0))
            .willReturn(List.of(place));
        given(reviewService.getAverageRatings(List.of(1L))).willReturn(Map.of());
        given(markerColorService.calculateMarkerColor(null, null)).willReturn(MarkerColor.GREY);

        List<PlaceMarkerResponse> result = placeService.searchByCategory(
            null, null, null,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0
        );

        assertThat(result).extracting(PlaceMarkerResponse::getPlaceId).containsExactly(1L);
        verifyNoInteractions(placeCategoryService, petRepository, favoriteService);
    }

    @Test
    void rejectsUnknownCategory() {
        given(placeCategoryService.getActiveCategory("UNKNOWN"))
            .willThrow(new BaseException(ErrorCode.INVALID_CATEGORY));

        assertThatThrownBy(() -> placeService.searchByCategory(
            "UNKNOWN", null, null,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CATEGORY));

        verifyNoInteractions(placeRepository);
    }

    private Place place(Long id, double mapX, double mapY) {
        Place place = mock(Place.class);
        given(place.getId()).willReturn(id);
        given(place.getMapX()).willReturn(mapX);
        given(place.getMapY()).willReturn(mapY);
        return place;
    }
}
