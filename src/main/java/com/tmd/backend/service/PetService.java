package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.image.ImageUsage;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.pet.PetSize;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.request.pet.PetRegisterRequest;
import com.tmd.backend.dto.request.pet.PetUpdateRequest;
import com.tmd.backend.dto.response.pet.PetResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PetService {
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final ImageService imageService;

    public List<PetResponse> getPets(String email) {
        User user = findUser(email);
        return petRepository.findByUserId(user.getId()).stream().map(this::toResponse).toList();
    }

    @Transactional
    public List<PetResponse> registerPets(String email, List<PetRegisterRequest> requests) {
        User user = findUser(email);
        List<Pet> pets = requests.stream().map(request -> Pet.create(
            user,
            request.getName(),
            parseBreed(request.getBreed()),
            parseSize(request.getSize()),
            imageService.attachReadyUpload(email, request.getImageUploadId(), ImageUsage.PET)
        )).toList();
        return petRepository.saveAll(pets).stream().map(this::toResponse).toList();
    }

    @Transactional
    public PetResponse updatePet(String email, Long petId, PetUpdateRequest request) {
        if (request.isRemoveImage() && request.getImageUploadId() != null) {
            throw new BaseException(ErrorCode.INVALID_IMAGE_UPLOAD);
        }
        Pet pet = petRepository.findByIdAndUserEmail(petId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.PET_NOT_FOUND));
        String previousImageKey = pet.getImageKey();
        String imageKey = previousImageKey;
        if (request.getImageUploadId() != null) {
            imageKey = imageService.attachReadyUpload(email, request.getImageUploadId(), ImageUsage.PET);
            imageService.markForDeletion(previousImageKey);
        } else if (request.isRemoveImage()) {
            imageKey = null;
            imageService.markForDeletion(previousImageKey);
        }
        pet.update(request.getName(), parseBreedNullable(request.getBreed()),
            parseSizeNullable(request.getSize()), imageKey);
        return toResponse(pet);
    }

    @Transactional
    public void deletePet(String email, Long petId) {
        Pet pet = petRepository.findByIdAndUserEmail(petId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.PET_NOT_FOUND));
        imageService.markForDeletion(pet.getImageKey());
        petRepository.delete(pet);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new BaseException(ErrorCode.UNAUTHORIZED));
    }

    private PetResponse toResponse(Pet pet) {
        return PetResponse.builder()
            .petId(pet.getId()).name(pet.getName()).breed(pet.getBreed().name())
            .size(pet.getSize().name()).imageUrl(imageService.toPublicUrl(pet.getImageKey())).build();
    }

    private PetBreed parseBreed(String value) {
        try { return PetBreed.valueOf(value); }
        catch (IllegalArgumentException | NullPointerException e) {
            throw new BaseException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private PetBreed parseBreedNullable(String value) {
        return value == null ? null : parseBreed(value);
    }

    private PetSize parseSize(String value) {
        try { return PetSize.valueOf(value); }
        catch (IllegalArgumentException | NullPointerException e) {
            throw new BaseException(ErrorCode.VALIDATION_ERROR);
        }
    }

    private PetSize parseSizeNullable(String value) {
        return value == null ? null : parseSize(value);
    }
}
