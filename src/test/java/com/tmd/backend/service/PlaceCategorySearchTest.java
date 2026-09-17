package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.common.MarkerColor;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.PlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PlaceCategorySearchTest {

    @Mock PlaceRepository placeRepository;
    @Mock PetRepository petRepository;
    @Mock MarkerColorService markerColorService;
    @Mock ReviewService reviewService;
    @Mock FavoriteService favoriteService;

    @InjectMocks PlaceService placeService;

    @Test
    @DisplayName("화면 안의 카테고리 장소를 현재 위치에서 가까운 순서로 반환")
    void returnsPlacesOrderedByDistance() {
        Pet pet = mock(Pet.class);
        Place farPlace = place(2L, "먼 카페", 127.10, 37.10);
        Place nearPlace = place(1L, "가까운 카페", 127.01, 37.01);

        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(pet));
        given(markerColorService.calculateMarkerColor(null, pet))
            .willReturn(MarkerColor.GREY);
        given(reviewService.getAverageRatings(List.of(2L, 1L)))
            .willReturn(Map.of(1L, 4.5));
        given(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, "FD", "FD05", null
        )).willReturn(List.of(farPlace, nearPlace));

        List<PlaceMarkerResponse> result = placeService.searchByCategory(
            " 카페 ", "test@email.com", 1L,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0
        );

        assertThat(result).extracting(PlaceMarkerResponse::getPlaceId)
            .containsExactly(1L, 2L);
        assertThat(result).extracting(PlaceMarkerResponse::getAverageRating)
            .containsExactly(4.5, 0.0);
    }

    @Test
    @DisplayName("여러 세부 코드에서 조회된 동일 장소를 한 번만 반환")
    void removesDuplicatesAcrossCategoryCodes() {
        Pet pet = mock(Pet.class);
        Place place = place(1L, "음식점", 127.01, 37.01);

        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(pet));
        given(markerColorService.calculateMarkerColor(null, pet))
            .willReturn(MarkerColor.GREY);
        given(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, "FD", "FD01", null
        )).willReturn(List.of(place));
        given(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, "FD", "FD02", null
        )).willReturn(List.of(place));
        given(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, "FD", "FD03", null
        )).willReturn(List.of());

        List<PlaceMarkerResponse> result = placeService.searchByCategory(
            "음식점", "test@email.com", 1L,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0
        );

        assertThat(result).extracting(PlaceMarkerResponse::getPlaceId)
            .containsExactly(1L);
    }

    @Test
    @DisplayName("잘못된 화면 좌표는 DB 조회 전에 거부")
    void rejectsInvalidBounds() {
        assertThatThrownBy(() -> placeService.searchByCategory(
            "카페", "test@email.com", 1L,
            38.0, 127.0, 37.0, 128.0,
            127.0, 37.0
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verifyNoInteractions(placeRepository, petRepository);
    }

    @Test
    @DisplayName("본인 소유가 아닌 반려견이면 장소를 조회하지 않음")
    void rejectsPetOwnedByAnotherUser() {
        given(petRepository.findByIdAndUserEmail(99L, "test@email.com"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.searchByCategory(
            "카페", "test@email.com", 99L,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.NOT_OWNER_OF_DOG));

        verify(placeRepository, never()).findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, "FD", "FD05", null
        );
    }

    @Test
    @DisplayName("비회원은 petId 없이 카테고리를 검색하고 회색 마커를 받음")
    void guestSearchReturnsGreyMarkers() {
        Place place = place(1L, "카페", 127.01, 37.01);
        given(placeRepository.findPlacesWithCategory(
            37.0, 127.0, 38.0, 128.0, "FD", "FD05", null
        )).willReturn(List.of(place));
        given(reviewService.getAverageRatings(List.of(1L))).willReturn(Map.of(1L, 4.0));
        given(markerColorService.calculateMarkerColor(null, null)).willReturn(MarkerColor.GREY);

        List<PlaceMarkerResponse> result = placeService.searchByCategory(
            "카페", null, null,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0
        );

        assertThat(result).singleElement().satisfies(marker -> {
            assertThat(marker.getMarkerColor()).isEqualTo("GREY");
            assertThat(marker.isFavorite()).isFalse();
        });
        verifyNoInteractions(petRepository, favoriteService);
    }

    @Test
    @DisplayName("전체 카테고리는 bbox 안의 모든 장소를 거리순으로 반환")
    void allCategoryReturnsEveryPlaceWithinBounds() {
        Place farPlace = place(2L, "먼 장소", 127.10, 37.10);
        Place nearPlace = place(1L, "가까운 장소", 127.01, 37.01);
        given(placeRepository.findPlacesWithinBounds(37.0, 127.0, 38.0, 128.0))
            .willReturn(List.of(farPlace, nearPlace));
        given(reviewService.getAverageRatings(List.of(2L, 1L))).willReturn(Map.of());
        given(markerColorService.calculateMarkerColor(null, null)).willReturn(MarkerColor.GREY);

        List<PlaceMarkerResponse> result = placeService.searchByCategory(
            " 전체 ", null, null,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0
        );

        assertThat(result).extracting(PlaceMarkerResponse::getPlaceId)
            .containsExactly(1L, 2L);
        verify(placeRepository, never()).findPlacesWithCategory(
            anyDouble(), anyDouble(), anyDouble(), anyDouble(),
            any(), any(), any()
        );
    }

    @Test
    @DisplayName("지원하지 않는 카테고리는 DB 조회 전에 거부")
    void rejectsUnknownCategory() {
        assertThatThrownBy(() -> placeService.searchByCategory(
            "없는 카테고리", null, null,
            37.0, 127.0, 38.0, 128.0,
            127.0, 37.0
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_CATEGORY));

        verifyNoInteractions(placeRepository, petRepository);
    }

    private Place place(Long id, String title, double mapX, double mapY) {
        Place place = mock(Place.class);
        given(place.getId()).willReturn(id);
        given(place.getTitle()).willReturn(title);
        given(place.getMapX()).willReturn(mapX);
        given(place.getMapY()).willReturn(mapY);
        return place;
    }
}
