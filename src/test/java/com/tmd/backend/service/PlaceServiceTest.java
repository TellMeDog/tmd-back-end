package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.external.TourApiClient;
import com.tmd.backend.external.TourApiPlaceItem;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.PlacePetInfoRepository;
import com.tmd.backend.repository.PlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock GridService gridService;
    @Mock PlaceRepository placeRepository;
    @Mock PetRepository petRepository;
    @Mock TourApiClient tourApiClient;
    @Mock PlacePetInfoRepository placePetInfoRepository;
    @Mock MarkerColorService markerColorService;
    @Mock KeywordCacheService keywordCacheService;
    @Mock ReviewService reviewService;

    @InjectMocks PlaceService placeService;

    // ===== searchByKeyword =====

    @Test
    @DisplayName("최초 요청 — Redis 락 획득 후 TourAPI 호출")
    void searchByKeyword_최초요청_API호출() {
        given(keywordCacheService.tryMarkFetched("강남")).willReturn(true);
        given(tourApiClient.getPlacesByKeyword("강남")).willReturn(List.of());
        given(placeRepository.findPlacesWithKeyword("강남")).willReturn(List.of());
        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(mock(Pet.class)));

        placeService.searchByKeyword("강남", 1L, "test@email.com");

        verify(tourApiClient).getPlacesByKeyword("강남");
    }

    @Test
    @DisplayName("중복 요청 — Redis 락 실패 시 TourAPI 호출 안 함")
    void searchByKeyword_중복요청_API호출안함() {
        given(keywordCacheService.tryMarkFetched("강남")).willReturn(false);
        given(placeRepository.findPlacesWithKeyword("강남")).willReturn(List.of());
        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(mock(Pet.class)));

        placeService.searchByKeyword("강남", 1L, "test@email.com");

        verify(tourApiClient, never()).getPlacesByKeyword("강남");
    }

    @Test
    @DisplayName("TourAPI 결과를 DB에 없으면 저장, 있으면 그냥 사용")
    void searchByKeyword_신규장소_DB저장() {
        TourApiPlaceItem newItem = mock(TourApiPlaceItem.class);
        given(newItem.getContentid()).willReturn("CONTENT1");
        given(newItem.getMapx()).willReturn("127.0");
        given(newItem.getMapy()).willReturn("37.0");

        given(keywordCacheService.tryMarkFetched("강남")).willReturn(true);
        given(tourApiClient.getPlacesByKeyword("강남")).willReturn(List.of(newItem));
        given(placeRepository.findByContentId("CONTENT1")).willReturn(Optional.empty());
        given(placeRepository.save(any(Place.class))).willReturn(mock(Place.class));
        given(placeRepository.findPlacesWithKeyword("강남")).willReturn(List.of());
        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(mock(Pet.class)));

        placeService.searchByKeyword("강남", 1L, "test@email.com");

        verify(placeRepository).save(any(Place.class));
    }

    @Test
    @DisplayName("본인 소유 아닌 petId면 NOT_OWNER_OF_DOG 예외")
    void searchByKeyword_타인pet_예외() {
        given(keywordCacheService.tryMarkFetched("강남")).willReturn(false);
        given(placeRepository.findPlacesWithKeyword("강남")).willReturn(List.of());
        given(petRepository.findByIdAndUserEmail(99L, "test@email.com"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.searchByKeyword("강남", 99L, "test@email.com"))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> org.assertj.core.api.Assertions
                .assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_OWNER_OF_DOG));
    }

}
