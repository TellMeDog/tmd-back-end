package com.tmd.backend.service.user;

import com.tmd.backend.service.image.ImageService;
import com.tmd.backend.service.review.ReviewService;

import com.tmd.backend.domain.user.AuthProvider;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.favorite.Favorite;
import org.springframework.data.domain.PageImpl;
import com.tmd.backend.repository.favorite.FavoriteRepository;
import com.tmd.backend.repository.pet.PetRepository;
import com.tmd.backend.repository.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PetRepository petRepository;
    @Mock FavoriteRepository favoriteRepository;
    @Mock ReviewService reviewService;
    @Mock ImageService imageService;
    @InjectMocks UserService userService;

    @Test
    void returnsOwnedPetIdsInAscendingOrder() {
        User user = mock(User.class);
        given(user.getId()).willReturn(7L);
        given(user.getEmail()).willReturn("user@example.com");
        given(user.getNickname()).willReturn("사용자");
        given(user.getProvider()).willReturn(AuthProvider.LOCAL);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(petRepository.findIdsByUserId(7L)).willReturn(List.of(2L, 5L, 9L));

        var response = userService.getMyInfo("user@example.com");

        assertThat(response.getPetIds()).containsExactly(2L, 5L, 9L);
        verify(petRepository).findIdsByUserId(7L);
    }

    @Test
    void returnsEmptyPetIdsWhenUserHasNoPets() {
        User user = mock(User.class);
        given(user.getId()).willReturn(7L);
        given(user.getProvider()).willReturn(AuthProvider.LOCAL);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(petRepository.findIdsByUserId(7L)).willReturn(List.of());

        assertThat(userService.getMyInfo("user@example.com").getPetIds()).isEmpty();
    }

    @Test
    void withdrawDeletesOwnedDataAndSchedulesImages() {
        User user = mock(User.class);
        Pet pet = mock(Pet.class);
        Favorite favorite = mock(Favorite.class);
        given(user.getId()).willReturn(7L);
        given(pet.getImageKey()).willReturn("images/pet/delete.jpg");
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(petRepository.findByUserId(7L)).willReturn(List.of(pet));
        given(favoriteRepository.findAllByUserId(7L, org.springframework.data.domain.Pageable.unpaged()))
            .willReturn(new PageImpl<>(List.of(favorite)));

        userService.withdraw("user@example.com");

        verify(imageService).markAllForDeletion(7L);
        verify(reviewService).deleteReviewsByUserId(7L);
        verify(imageService).markForDeletion("images/pet/delete.jpg");
        verify(petRepository).deleteAll(List.of(pet));
        verify(petRepository).flush();
        verify(favoriteRepository).deleteAllInBatch(List.of(favorite));
        verify(userRepository).delete(user);
    }
}
