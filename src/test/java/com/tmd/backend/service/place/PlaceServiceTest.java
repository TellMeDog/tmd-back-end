package com.tmd.backend.service.place;

import com.tmd.backend.service.favorite.FavoriteService;
import com.tmd.backend.service.review.ReviewService;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.common.MarkerColor;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.place.PlaceDetailResponse;
import com.tmd.backend.dto.response.place.PlaceMapMarkerResponse;
import com.tmd.backend.dto.response.place.PlaceMarkerResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.pet.PetRepository;
import com.tmd.backend.repository.place.PlaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceServiceTest {

    @Mock PlaceRepository placeRepository;
    @Mock PetRepository petRepository;
    @Mock MarkerColorService markerColorService;
    @Mock ReviewService reviewService;
    @Mock FavoriteService favoriteService;
    @Mock PlaceCategoryService placeCategoryService;

    @InjectMocks PlaceService placeService;

    // ===== init =====

    @Test
    @DisplayName("홈 지도는 DB에서 6km 이내의 가까운 장소 10개를 비회원용 회색 마커로 반환")
    void init_비회원_반경과개수와정렬적용() {
        List<Place> candidates = new ArrayList<>();
        LongStream.rangeClosed(1, 11)
            .mapToObj(id -> place(id, 127.0, 37.0 + id * 0.004))
            .forEach(candidates::add);
        Collections.reverse(candidates);
        candidates.add(place(99L, 127.0, 37.06));

        given(placeRepository.findPlacesWithinBounds(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).willReturn(candidates);
        given(reviewService.getAverageRatings(LongStream.rangeClosed(1, 10).boxed().toList()))
            .willReturn(Map.of());
        given(markerColorService.calculateMarkerColorForPlace(any(Place.class), isNull())).willReturn(MarkerColor.GREY);

        List<PlaceMarkerResponse> result = placeService.init(null, null, 127.0, 37.0);

        assertThat(result).extracting(PlaceMarkerResponse::getPlaceId)
            .containsExactlyElementsOf(LongStream.rangeClosed(1, 10).boxed().toList());
        assertThat(result).extracting(PlaceMarkerResponse::getDistance).isSorted();
        assertThat(result).allSatisfy(marker -> {
            assertThat(marker.getDistance()).isLessThanOrEqualTo(6_000);
            assertThat(marker.getMarkerColor()).isEqualTo("GREY");
            assertThat(marker.isFavorite()).isFalse();
        });
        verifyNoInteractions(petRepository, favoriteService);
    }

    @Test
    @DisplayName("홈 지도는 반려동물과 로그인 사용자의 마커색 및 즐겨찾기를 반영")
    void init_회원_반려동물과즐겨찾기반영() {
        Pet pet = mock(Pet.class);
        Place place = place(1L, 127.0, 37.01);
        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(pet));
        given(placeRepository.findPlacesWithinBounds(
            anyDouble(), anyDouble(), anyDouble(), anyDouble()
        )).willReturn(List.of(place));
        given(reviewService.getAverageRatings(List.of(1L))).willReturn(Map.of(1L, 4.5));
        given(markerColorService.calculateMarkerColorForPlace(any(Place.class), eq(pet))).willReturn(MarkerColor.GREEN);
        given(favoriteService.getFavoritePlaceIds("test@email.com", List.of(1L)))
            .willReturn(Set.of(1L));

        List<PlaceMarkerResponse> result = placeService.init(
            "test@email.com", 1L, 127.0, 37.0
        );

        assertThat(result).singleElement().satisfies(marker -> {
            assertThat(marker.getMarkerColor()).isEqualTo("GREEN");
            assertThat(marker.isFavorite()).isTrue();
            assertThat(marker.getAverageRating()).isEqualTo(4.5);
        });
    }

    @Test
    @DisplayName("홈 지도는 잘못된 현재 좌표를 DB 조회 전에 거부")
    void init_잘못된좌표_예외() {
        assertThatThrownBy(() -> placeService.init(null, null, Double.NaN, 37.0))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));

        verifyNoInteractions(placeRepository, petRepository);
    }

    // ===== searchByKeyword =====

    @Test
    void keywordMarkerSearchReturnsAllOneThousandLightweightMarkers() {
        List<Place> places = LongStream.rangeClosed(1, 1_000)
            .mapToObj(id -> place(id, 127.0, 37.0))
            .toList();
        given(placeRepository.findPlacesWithKeyword("공원")).willReturn(places);
        given(markerColorService.calculateMarkerColorForPlace(any(Place.class), isNull())).willReturn(MarkerColor.GREY);

        List<PlaceMapMarkerResponse> result = placeService.searchByKeyword("공원", null, null);

        assertThat(result).hasSize(1_000);
        assertThat(result).extracting(PlaceMapMarkerResponse::placeId).isSorted();
        verifyNoInteractions(reviewService, favoriteService);
    }

    @Test
    @DisplayName("키워드 검색은 DB에 저장된 장소만 조회")
    void searchByKeyword_DB조회() {
        given(placeRepository.findPlacesWithKeyword("강남")).willReturn(List.of());
        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(mock(Pet.class)));

        placeService.searchByKeyword("강남", 1L, "test@email.com");

        verify(placeRepository).findPlacesWithKeyword("강남");
    }

    @Test
    @DisplayName("키워드 앞뒤 공백 제거")
    void searchByKeyword_검색어공백제거() {
        given(placeRepository.findPlacesWithKeyword("강남")).willReturn(List.of());
        given(petRepository.findByIdAndUserEmail(1L, "test@email.com"))
            .willReturn(Optional.of(mock(Pet.class)));

        placeService.searchByKeyword(" 강남 ", 1L, "test@email.com");

        verify(placeRepository).findPlacesWithKeyword("강남");
    }

    @Test
    @DisplayName("빈 키워드는 장소를 조회하지 않고 예외")
    void searchByKeyword_빈키워드_예외() {
        assertThatThrownBy(() -> placeService.searchByKeyword(" ", 1L, "test@email.com"))
            .isInstanceOf(BaseException.class);

        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("본인 소유 아닌 petId면 NOT_OWNER_OF_DOG 예외")
    void searchByKeyword_타인pet_예외() {
        given(petRepository.findByIdAndUserEmail(99L, "test@email.com"))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> placeService.searchByKeyword("강남", 99L, "test@email.com"))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> org.assertj.core.api.Assertions
                .assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_OWNER_OF_DOG));

        verifyNoInteractions(placeRepository);
    }

    @Test
    @DisplayName("지도 마커 검색은 경량 마커만 반환하고 평점과 즐겨찾기를 조회하지 않음")
    void searchByKeyword_petId없으면_회색마커() {
        Place place = mock(Place.class);
        given(place.getId()).willReturn(1L);
        given(place.getMapX()).willReturn(127.0);
        given(place.getMapY()).willReturn(37.0);
        given(placeRepository.findPlacesWithKeyword("강남")).willReturn(List.of(place));
        given(markerColorService.calculateMarkerColorForPlace(any(Place.class), isNull())).willReturn(MarkerColor.GREY);

        List<PlaceMapMarkerResponse> result = placeService.searchByKeyword("강남", null, "test@email.com");

        assertThat(result).singleElement().satisfies(marker -> {
            assertThat(marker.markerColor()).isEqualTo("GREY");
            assertThat(marker.placeId()).isEqualTo(1L);
        });
        verifyNoInteractions(petRepository, reviewService, favoriteService);
    }

    @Test
    @DisplayName("비회원이 petId를 전달하면 NOT_OWNER_OF_DOG 예외")
    void searchByKeyword_비회원petId_예외() {
        assertThatThrownBy(() -> placeService.searchByKeyword("강남", 1L, null))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOT_OWNER_OF_DOG));

        verifyNoInteractions(petRepository, placeRepository);
    }

    @Test
    void keywordListReturnsPagedPlacesInRepositoryOrder() {
        Place first = place(1L, 127.01, 37.01);
        Place second = place(2L, 127.02, 37.02);
        PageRequest pageable = PageRequest.of(0, 2);
        given(placeRepository.findPlacePageWithKeyword("공원", 127.0, 37.0, pageable))
            .willReturn(new PageImpl<>(List.of(first, second), pageable, 1_000));
        given(reviewService.getAverageRatings(List.of(1L, 2L))).willReturn(Map.of(1L, 4.5));
        given(markerColorService.calculateMarkerColorForPlace(any(Place.class), isNull())).willReturn(MarkerColor.GREY);

        PageResponse<PlaceMarkerResponse> result = placeService.searchKeywordList(
            "공원", null, null, 127.0, 37.0, 0, 2
        );

        assertThat(result.getContent()).extracting(PlaceMarkerResponse::getPlaceId)
            .containsExactly(1L, 2L);
        assertThat(result.getTotalElements()).isEqualTo(1_000);
        assertThat(result.isHasNext()).isTrue();
        verifyNoInteractions(favoriteService);
    }

    @Test
    @DisplayName("비활성화된 장소는 상세 조회에서 찾을 수 없음")
    void getPlaceDetail_비활성장소_예외() {
        Place place = mock(Place.class);
        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(place.isActive()).willReturn(false);

        assertThatThrownBy(() -> placeService.getPlaceDetail(
            1L, 1L, "test@email.com", 127.0, 37.0, 10, "latest"
        ))
            .isInstanceOf(BaseException.class)
            .satisfies(e -> org.assertj.core.api.Assertions
                .assertThat(((BaseException) e).getErrorCode())
                .isEqualTo(ErrorCode.PLACE_NOT_FOUND));

        verifyNoInteractions(petRepository, reviewService, favoriteService);
    }

    @Test
    @DisplayName("비회원은 회색 마커와 전체 리뷰로 장소 상세를 조회")
    void getPlaceDetail_비회원_성공() {
        Place place = mock(Place.class);
        given(place.getId()).willReturn(1L);
        given(place.isActive()).willReturn(true);
        given(place.getMapX()).willReturn(127.0);
        given(place.getMapY()).willReturn(37.0);
        given(placeRepository.findById(1L)).willReturn(Optional.of(place));
        given(markerColorService.calculateMarkerColorForPlace(any(Place.class), isNull())).willReturn(MarkerColor.GREY);
        given(reviewService.getAverageRating(1L)).willReturn(4.0);
        given(reviewService.getPlaceReviewList(1L, null, 0, 10, "latest"))
            .willReturn(new PageResponse<>(List.of(), 0, 10, 0, 0, false));

        PlaceDetailResponse result = placeService.getPlaceDetail(
            1L, null, null, 127.0, 37.0, 10, "latest"
        );

        assertThat(result.getPlaceMarkerResponse().getMarkerColor()).isEqualTo("GREY");
        assertThat(result.getPlaceMarkerResponse().isFavorite()).isFalse();
        assertThat(result.getMyReviews()).isEmpty();
        verify(reviewService, never()).getMyReviewListInPlace(anyLong(), anyString());
        verifyNoInteractions(petRepository, favoriteService);
    }

    private Place place(Long id, double mapX, double mapY) {
        Place place = mock(Place.class);
        lenient().when(place.getId()).thenReturn(id);
        given(place.getMapX()).willReturn(mapX);
        given(place.getMapY()).willReturn(mapY);
        return place;
    }

}
