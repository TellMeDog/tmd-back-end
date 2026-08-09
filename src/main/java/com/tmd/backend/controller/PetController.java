package com.tmd.backend.controller;


import com.tmd.backend.dto.request.pet.PetRegisterRequest;
import com.tmd.backend.dto.request.pet.PetUpdateRequest;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.pet.PetResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/pets")
public class PetController {

    private final AtomicLong SEQUENCE = new AtomicLong(1L);

    @GetMapping
    public ResponseEntity<SuccessResponseDto<List<PetResponse>>> getPetsList(){
        List<PetResponse> dummy = List.of(
            PetResponse.builder()
                .petId(1L)
                .name("멍이")
                .breed("MALTESE")
                .size("SMALL")
                .imageUrl("https://cloudflareR2.jpg")
                .build());
        return ResponseEntity.ok(SuccessResponseDto.success("반려견 목록을 조회했습니다.", dummy));
    }

    @PostMapping
    public ResponseEntity<SuccessResponseDto<List<PetResponse>>> registerPets(@Valid @RequestBody List<PetRegisterRequest> requests){

        List<PetResponse> responses = requests.stream()
            .map(req -> PetResponse.builder()
                .petId(SEQUENCE.getAndIncrement())
                .name(req.getName())
                .breed(req.getBreed())
                .size(req.getSize())
                .imageUrl(req.getImageUrl())
                .build())
            .toList();

        return ResponseEntity.ok(SuccessResponseDto.success("반려견 정보가 등록되었습니다.", responses));
    }

    @PatchMapping("/{petId}")
    public ResponseEntity<SuccessResponseDto<PetResponse>> updatePet(@PathVariable Long petId,
                                                                     @RequestBody PetUpdateRequest request){

        PetResponse response = PetResponse.builder()
            .petId(petId)
            .name(request.getName() != null ? request.getName() : "초코")
            .breed(request.getBreed() != null ? request.getBreed() : "MALTESE")
            .size(request.getSize() != null ? request.getSize() : "SMALL")
            .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : "https://placehold.co/400x400")
            .build();

        return ResponseEntity.ok(SuccessResponseDto.success("반려견 정보가 수정되었습니다.", response));
    }


    @DeleteMapping("/{petId}")
    public ResponseEntity<SuccessResponseDto<Void>> deletePet(@PathVariable Long petId){
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("반려견 정보가 삭제되었습니다."));
    }
}
