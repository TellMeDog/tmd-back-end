package com.tmd.backend.service.place;

import com.tmd.backend.service.favorite.FavoriteService;
import com.tmd.backend.service.review.ReviewService;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.common.MarkerColor;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.TourCategory;
import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.place.PlaceMapMarkerResponse;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.pet.PetRepository;
import com.tmd.backend.repository.place.PlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
    void searchesByOfficialCategoryCodeAndReturnsLightweightMarkers() {
        TourCategory category = mock(TourCategory.class);
        Pet pet = mock(Pet.class);
        Place farPlace = place(2L, 127.10, 37.10);
        Place nearPlace = place(1L, 127.01, 37.01);
        given(category.getDepth()).willReturn(2);
        given(category.getCode()).willReturn("FD05");
        given(placeCategoryService.getActiveCategory("FD05")).willReturn(category);
        given(petRepository.findByIdAndUserEmail(1L, "test@email.com")).willReturn(Optional.of(pet));
        given(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, 2, "FD05"
        )).willReturn(List.of(farPlace, nearPlace));
        given(markerColorService.calculateMarkerColor(null, pet)).willReturn(MarkerColor.GREY);

        List<PlaceMapMarkerResponse> result = placeService.searchByCategory(
            "FD05", "test@email.com", 1L,
            37.0, 127.0, 38.0, 128.0
        );

        assertThat(result).extracting(PlaceMapMarkerResponse::placeId).containsExactly(1L, 2L);
        verifyNoInteractions(reviewService, favoriteService);
    }

    @Test
    void omittedCategoryReturnsEveryPlaceWithinBounds() {
        Place farPlace = place(2L, 127.10, 37.10);
        Place nearPlace = place(1L, 127.01, 37.01);
        given(placeRepository.findPlacesWithinBounds(37.0, 127.0, 38.0, 128.0))
            .willReturn(List.of(farPlace, nearPlace));
        given(markerColorService.calculateMarkerColor(null, null)).willReturn(MarkerColor.GREY);

        List<PlaceMapMarkerResponse> result = placeService.searchByCategory(
            null, null, null,
            37.0, 127.0, 38.0, 128.0
        );

        assertThat(result).extracting(PlaceMapMarkerResponse::placeId).containsExactly(1L, 2L);
        verifyNoInteractions(placeCategoryService, petRepository, favoriteService);
        verify(placeRepository, never()).findPlacesWithCategory(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), anyString()
        );
    }

    @Test
    void guestCategorySearchReturnsGreyMarkers() {
        TourCategory category = mock(TourCategory.class);
        Place place = place(1L, 127.01, 37.01);
        given(category.getDepth()).willReturn(1);
        given(category.getCode()).willReturn("FD");
        given(placeCategoryService.getActiveCategory("FD")).willReturn(category);
        given(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, 1, "FD"
        )).willReturn(List.of(place));
        given(markerColorService.calculateMarkerColor(null, null)).willReturn(MarkerColor.GREY);

        List<PlaceMapMarkerResponse> result = placeService.searchByCategory(
            "FD", null, null,
            37.0, 127.0, 38.0, 128.0
        );

        assertThat(result).singleElement().satisfies(marker -> {
            assertThat(marker.markerColor()).isEqualTo("GREY");
        });
        verifyNoInteractions(petRepository, reviewService, favoriteService);
    }

    @Test
    void categoryListReturnsRequestedPageWithBatchRatingsAndFavorites() {
        TourCategory category = mock(TourCategory.class);
        Place first = place(1L, 127.01, 37.01);
        Place second = place(2L, 127.02, 37.02);
        given(category.getDepth()).willReturn(2);
        given(category.getCode()).willReturn("FD05");
        given(placeCategoryService.getActiveCategory("FD05")).willReturn(category);
        given(placeRepository.findPlacePageWithCategory(
            37.0, 127.0, 38.0, 128.0, 2, "FD05", 127.0, 37.0, PageRequest.of(0, 2)
        )).willReturn(new PageImpl<>(List.of(first, second), PageRequest.of(0, 2), 1_000));
        given(reviewService.getAverageRatings(List.of(1L, 2L))).willReturn(Map.of(1L, 4.5));
        given(favoriteService.getFavoritePlaceIds("test@email.com", List.of(1L, 2L)))
            .willReturn(Set.of(2L));
        given(markerColorService.calculateMarkerColor(null, null)).willReturn(MarkerColor.GREY);

        PageResponse<PlaceMarkerResponse> result = placeService.searchCategoryList(
            "FD05", "test@email.com", null,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0, 0, 2
        );

        assertThat(result.getContent()).extracting(PlaceMarkerResponse::getPlaceId)
            .containsExactly(1L, 2L);
        assertThat(result.getContent()).extracting(PlaceMarkerResponse::isFavorite)
            .containsExactly(false, true);
        assertThat(result.getTotalElements()).isEqualTo(1_000);
        assertThat(result.getTotalPages()).isEqualTo(500);
        assertThat(result.isHasNext()).isTrue();
        verify(reviewService).getAverageRatings(List.of(1L, 2L));
        verify(favoriteService).getFavoritePlaceIds("test@email.com", List.of(1L, 2L));
    }

    @Test
    void categoryListRejectsInvalidPageSizeBeforeDatabaseQuery() {
        assertThatThrownBy(() -> placeService.searchCategoryList(
            null, null, null,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0, 0, 101
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verifyNoInteractions(placeRepository);
    }

    @Test
    void rejectsInvalidBoundsBeforeCategoryLookup() {
        assertThatThrownBy(() -> placeService.searchByCategory(
            "FD", null, null,
            38.0, 127.0, 37.0, 128.0
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verifyNoInteractions(placeCategoryService, placeRepository, petRepository);
    }

    @Test
    void rejectsUnknownCategory() {
        given(placeCategoryService.getActiveCategory("UNKNOWN"))
            .willThrow(new BaseException(ErrorCode.INVALID_CATEGORY));

        assertThatThrownBy(() -> placeService.searchByCategory(
            "UNKNOWN", null, null,
            37.0, 127.0, 38.0, 128.0
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CATEGORY));

        verifyNoInteractions(placeRepository);
    }

    @Test
    void rejectsPetOwnedByAnotherUserBeforeCategoryLookup() {
        given(petRepository.findByIdAndUserEmail(99L, "test@email.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.searchByCategory(
            "FD", "test@email.com", 99L,
            37.0, 127.0, 38.0, 128.0
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOT_OWNER_OF_DOG));

        verifyNoInteractions(placeCategoryService, placeRepository);
    }

    private Place place(Long id, double mapX, double mapY) {
        Place place = mock(Place.class);
        given(place.getId()).willReturn(id);
        given(place.getMapX()).willReturn(mapX);
        given(place.getMapY()).willReturn(mapY);
        return place;
    }
}
