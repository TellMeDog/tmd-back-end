package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.favorite.Favorite;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetPolicy;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.FavoriteRepository;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.PlaceRepository;
import com.tmd.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {
    @Mock FavoriteRepository favoriteRepository;
    @Mock UserRepository userRepository;
    @Mock PlaceRepository placeRepository;
    @Mock PetRepository petRepository;
    @Mock MarkerColorService markerColorService;

    @Test
    void returnsFavoritesWithSelectedPetsMarkerColorAndPageMetadata() {
        FavoriteService service = service();
        User user = mock(User.class);
        Pet pet = mock(Pet.class);
        Favorite favorite = mock(Favorite.class);
        Place place = mock(Place.class);
        PlacePetPolicy policy = mock(PlacePetPolicy.class);
        PageRequest pageable = PageRequest.of(0, 10);

        given(user.getId()).willReturn(1L);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(petRepository.findByIdAndUserEmail(2L, "user@example.com"))
            .willReturn(Optional.of(pet));
        given(favorite.getPlace()).willReturn(place);
        given(place.getId()).willReturn(10L);
        given(place.getFirstImage()).willReturn("https://example.com/place.jpg");
        given(place.getTitle()).willReturn("반려견 공원");
        given(place.getAddr1()).willReturn("서울시");
        given(place.getPlacePetPolicy()).willReturn(policy);
        given(markerColorService.calculateMarkerColor(policy, pet)).willReturn("GREEN");
        given(favoriteRepository.findAllByUserId(
            org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.any(PageRequest.class)))
            .willReturn(new PageImpl<>(List.of(favorite), pageable, 1));

        var result = service.getMyFavorites("user@example.com", 2L, 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getPlaceId()).isEqualTo(10L);
        assertThat(result.getContent().getFirst().getMarkerColor()).isEqualTo("GREEN");
        assertThat(result.getPage()).isZero();
        assertThat(result.getSize()).isEqualTo(10);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.isHasNext()).isFalse();
    }

    @Test
    void rejectsInvalidPaginationBeforeDatabaseAccess() {
        assertThatThrownBy(() -> service().getMyFavorites("user@example.com", 2L, -1, 10))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }

    @Test
    void rejectsDuplicateFavorite() {
        FavoriteService service = service();
        User user = mock(User.class);
        Place place = mock(Place.class);
        given(user.getId()).willReturn(1L);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(placeRepository.findById(10L)).willReturn(Optional.of(place));
        given(favoriteRepository.existsByUserIdAndPlaceId(1L, 10L)).willReturn(true);

        assertThatThrownBy(() -> service.addFavorite("user@example.com", 10L))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.ALREADY_FAVORITE));
    }

    @Test
    void deletesFavoriteOwnedByUser() {
        FavoriteService service = service();
        User user = mock(User.class);
        Favorite favorite = mock(Favorite.class);
        given(user.getId()).willReturn(1L);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(favoriteRepository.findByUserIdAndPlaceId(1L, 10L))
            .willReturn(Optional.of(favorite));

        service.deleteFavorite("user@example.com", 10L);

        ArgumentCaptor<Favorite> captor = ArgumentCaptor.captor();
        verify(favoriteRepository).delete(captor.capture());
        assertThat(captor.getValue()).isSameAs(favorite);
    }

    private FavoriteService service() {
        return new FavoriteService(
            favoriteRepository,
            userRepository,
            placeRepository,
            petRepository,
            markerColorService
        );
    }
}
