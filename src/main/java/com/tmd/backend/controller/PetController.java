package com.tmd.backend.controller;

import com.tmd.backend.dto.request.pet.PetRegisterRequest;
import com.tmd.backend.dto.request.pet.PetUpdateRequest;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.pet.PetResponse;
import com.tmd.backend.service.pet.PetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Pet", description = "반려동물 API")
@Slf4j
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {
    private final PetService petService;

    @Operation(
        summary = "반려동물 조회",
        description = "내 반려동물 조회시 사용할 API"
    )
    @GetMapping
    public ResponseEntity<SuccessResponseDto<List<PetResponse>>> getPetsList(
        @AuthenticationPrincipal String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "반려견 목록을 조회했습니다.", petService.getPets(email)));
    }

    @Operation(
        summary = "반려동물 추가",
        description = "내 반려동물 추가시 사용할 API"
    )
    @PostMapping
    public ResponseEntity<SuccessResponseDto<List<PetResponse>>> registerPets(
        @RequestBody List<@Valid PetRegisterRequest> requests,
        @AuthenticationPrincipal String email
    ) {
        log.info("반려견 정보 등록 email : {}", email);
        return ResponseEntity.ok(SuccessResponseDto.success(
            "반려견 정보를 등록했습니다.", petService.registerPets(email, requests)));
    }

    @Operation(
        summary = "반려동물 수정",
        description = "내 반려동물 수정시 사용할 API"
    )
    @PutMapping("/{petId}")
    public ResponseEntity<SuccessResponseDto<PetResponse>> updatePet(
        @Parameter(description = "반려견 ID", example = "1") @PathVariable Long petId,
        @Valid @RequestBody PetUpdateRequest request,
        @AuthenticationPrincipal String email
    ) {
        log.info("반려견 정보 수정 email : {}", email);
        return ResponseEntity.ok(SuccessResponseDto.success(
            "반려견 정보를 수정했습니다.", petService.updatePet(email, petId, request)));
    }

    @Operation(
        summary = "반려동물 삭제",
        description = "내 반려동물 삭제시 사용할 API"
    )
    @DeleteMapping("/{petId}")
    public ResponseEntity<SuccessResponseDto<Void>> deletePet(
        @Parameter(description = "반려견 ID", example = "1") @PathVariable Long petId,
        @AuthenticationPrincipal String email
    ) {
        log.info("반려견 정보 삭제 email : {}", email);
        petService.deletePet(email, petId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("반려견 정보를 삭제했습니다."));
    }
}
