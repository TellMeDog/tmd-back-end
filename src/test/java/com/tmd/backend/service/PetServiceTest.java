package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.image.ImageUsage;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.request.pet.PetRegisterRequest;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private PetRepository petRepository;
    @Mock private ImageService imageService;

    @Test
    void registersPetWithOwnedReadyImageUpload() {
        PetService petService = new PetService(userRepository, petRepository, imageService);
        User user = mock(User.class);
        PetRegisterRequest request = mock(PetRegisterRequest.class);
        UUID uploadId = UUID.randomUUID();
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(request.getName()).willReturn("멍이");
        given(request.getBreed()).willReturn("시바견");
        given(request.getSize()).willReturn("SMALL");
        given(request.getImageUploadId()).willReturn(uploadId);
        given(imageService.attachReadyUpload("user@example.com", uploadId, ImageUsage.PET))
            .willReturn("images/pet/test.jpg");
        given(imageService.toPublicUrl("images/pet/test.jpg"))
            .willReturn("https://images.example.com/images/pet/test.jpg");
        given(petRepository.saveAll(org.mockito.ArgumentMatchers.<List<Pet>>any()))
            .willAnswer(invocation -> invocation.getArgument(0));

        var responses = petService.registerPets("user@example.com", List.of(request));

        ArgumentCaptor<List<Pet>> captor = ArgumentCaptor.captor();
        verify(petRepository).saveAll(captor.capture());
        assertThat(captor.getValue().getFirst().getBreed()).isEqualTo(PetBreed.SHIBA_INU);
        assertThat(captor.getValue().getFirst().getImageKey()).isEqualTo("images/pet/test.jpg");
        assertThat(responses.getFirst().getBreed()).isEqualTo("시바견");
        assertThat(responses.getFirst().getImageUrl())
            .isEqualTo("https://images.example.com/images/pet/test.jpg");
    }

    @Test
    void rejectsUnknownKoreanBreed() {
        assertInvalidBreed("없는견종");
    }

    @Test
    void rejectsEnglishEnumName() {
        assertInvalidBreed("SHIBA_INU");
    }

    private void assertInvalidBreed(String breed) {
        PetService petService = new PetService(userRepository, petRepository, imageService);
        User user = mock(User.class);
        PetRegisterRequest request = mock(PetRegisterRequest.class);
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(request.getBreed()).willReturn(breed);

        assertThatThrownBy(() -> petService.registerPets("user@example.com", List.of(request)))
            .isInstanceOf(BaseException.class)
            .satisfies(exception -> assertThat(((BaseException) exception).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_ERROR));
    }
}
