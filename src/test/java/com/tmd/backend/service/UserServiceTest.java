package com.tmd.backend.service;

import com.tmd.backend.domain.user.AuthProvider;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.repository.FavoriteRepository;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.UserRepository;
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
}
