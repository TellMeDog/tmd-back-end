package com.tmd.backend.service.pet;

import com.tmd.backend.service.image.ImageService;
import com.tmd.backend.service.review.ReviewService;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.image.ImageUsage;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.request.pet.PetRegisterRequest;
import com.tmd.backend.dto.request.pet.PetUpdateRequest;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.pet.PetRepository;
import com.tmd.backend.repository.user.UserRepository;
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
    @Mock private ReviewService reviewService;

    @Test
    void registersPetWithOwnedReadyImageUpload() {
        PetService petService = new PetService(userRepository, petRepository, imageService, reviewService);
        User user = mock(User.class);
        PetRegisterRequest request = mock(PetRegisterRequest.class);
        UUID uploadId = UUID.randomUUID();
        given(userRepository.findByEmail("user@example.com")).willReturn(Optional.of(user));
        given(request.getName()).willReturn("멍이");
        given(request.getBreed()).willReturn("시바견");
        given(request.getWeight()).willReturn(8.5);
        given(request.isHasMuzzle()).willReturn(false);
        given(request.isHasLeash()).willReturn(true);
        given(request.isHasCarrier()).willReturn(true);
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
        assertThat(captor.getValue().getFirst().getWeight()).isEqualTo(8.5);
        assertThat(captor.getValue().getFirst().isHasLeash()).isTrue();
        assertThat(captor.getValue().getFirst().isHasCarrier()).isTrue();
        assertThat(captor.getValue().getFirst().getImageKey()).isEqualTo("images/pet/test.jpg");
        assertThat(responses.getFirst().getBreed()).isEqualTo("시바견");
        assertThat(responses.getFirst().getWeight()).isEqualTo(8.5);
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

    @Test
    void updatesWeightAndEquipmentAndRemovesImage() {
        PetService petService = new PetService(userRepository, petRepository, imageService, reviewService);
        User user = mock(User.class);
        Pet pet = Pet.create(
            user, "멍이", PetBreed.SHIBA_INU, 8.5, "images/pet/old.jpg", false, false, false);
        PetUpdateRequest request = mock(PetUpdateRequest.class);
        given(request.isRemoveImage()).willReturn(true);
        given(request.getWeight()).willReturn(9.2);
        given(request.getHasMuzzle()).willReturn(true);
        given(request.getHasLeash()).willReturn(true);
        given(request.getHasCarrier()).willReturn(false);
        given(petRepository.findByIdAndUserEmail(1L, "user@example.com"))
            .willReturn(Optional.of(pet));

        var response = petService.updatePet("user@example.com", 1L, request);

        assertThat(pet.getWeight()).isEqualTo(9.2);
        assertThat(pet.getImageKey()).isNull();
        assertThat(pet.isHasMuzzle()).isTrue();
        assertThat(pet.isHasLeash()).isTrue();
        assertThat(pet.isHasCarrier()).isFalse();
        assertThat(response.getImageUrl()).isNull();
        verify(imageService).markForDeletion("images/pet/old.jpg");
    }

    @Test
    void deletesPetReviewsAndImagesBeforeDeletingPet() {
        PetService petService = new PetService(userRepository, petRepository, imageService, reviewService);
        Pet pet = mock(Pet.class);
        given(pet.getImageKey()).willReturn("images/pet/delete.jpg");
        given(petRepository.findByIdAndUserEmail(1L, "user@example.com"))
            .willReturn(Optional.of(pet));

        petService.deletePet("user@example.com", 1L);

        verify(reviewService).deleteReviewsByPetId(1L);
        verify(imageService).markForDeletion("images/pet/delete.jpg");
        verify(petRepository).delete(pet);
    }

    private void assertInvalidBreed(String breed) {
        PetService petService = new PetService(userRepository, petRepository, imageService, reviewService);
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
