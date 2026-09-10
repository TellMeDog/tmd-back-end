package com.tmd.backend.controller;

import com.tmd.backend.dto.request.pet.PetRegisterRequest;
import com.tmd.backend.dto.request.pet.PetUpdateRequest;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.pet.PetResponse;
import com.tmd.backend.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {
    private final PetService petService;

    @GetMapping
    public ResponseEntity<SuccessResponseDto<List<PetResponse>>> getPetsList(
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "반려견 목록을 조회했습니다.", petService.getPets(email)));
    }

    @PostMapping
    public ResponseEntity<SuccessResponseDto<List<PetResponse>>> registerPets(
        @Valid @RequestBody List<PetRegisterRequest> requests,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "반려견 정보를 등록했습니다.", petService.registerPets(email, requests)));
    }

    @PatchMapping("/{petId}")
    public ResponseEntity<SuccessResponseDto<PetResponse>> updatePet(
        @PathVariable Long petId,
        @Valid @RequestBody PetUpdateRequest request,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "반려견 정보를 수정했습니다.", petService.updatePet(email, petId, request)));
    }

    @DeleteMapping("/{petId}")
    public ResponseEntity<SuccessResponseDto<Void>> deletePet(
        @PathVariable Long petId,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        petService.deletePet(email, petId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("반려견 정보를 삭제했습니다."));
    }
}
