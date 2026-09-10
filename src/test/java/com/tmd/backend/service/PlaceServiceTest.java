package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.external.TourApiClient;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

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
    @DisplayName("키워드 검색은 DB에 저장된 장소만 조회")
    void searchByKeyword_DB조회() {
        given(placeRepository.findPlacesWithKeyword("강남")).willReturn(List.of());
        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(mock(Pet.class)));

        placeService.searchByKeyword("강남", 1L, "test@email.com", 127.0, 37.0);

        verify(placeRepository).findPlacesWithKeyword("강남");
        verifyNoInteractions(tourApiClient);
    }

    @Test
    @DisplayName("키워드 앞뒤 공백 제거")
    void searchByKeyword_검색어공백제거() {
        given(placeRepository.findPlacesWithKeyword("강남")).willReturn(List.of());
        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(mock(Pet.class)));

        placeService.searchByKeyword(" 강남 ", 1L, "test@email.com", 127.0, 37.0);

        verify(placeRepository).findPlacesWithKeyword("강남");
    }

    @Test
    @DisplayName("빈 키워드는 장소를 조회하지 않고 예외")
    void searchByKeyword_빈키워드_예외() {
        assertThatThrownBy(() -> placeService.searchByKeyword(" ", 1L, "test@email.com", 127.0, 37.0))
            .isInstanceOf(BaseException.class);

        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("본인 소유 아닌 petId면 NOT_OWNER_OF_DOG 예외")
    void searchByKeyword_타인pet_예외() {
        given(petRepository.findByIdAndUserEmail(99L, "test@email.com"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.searchByKeyword("강남", 99L, "test@email.com", 127.0, 37.0))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> org.assertj.core.api.Assertions
                .assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_OWNER_OF_DOG));

        verifyNoInteractions(placeRepository);
    }

}
