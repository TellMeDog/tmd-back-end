package com.tmd.backend.controller;


import com.tmd.backend.dto.request.pet.PetRegisterRequest;
import com.tmd.backend.dto.request.pet.PetUpdateRequest;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.pet.PetResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// 유저 - 반려견 목록에 필요한 import 추가
import com.tmd.backend.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor  // 클래스 안에 있는 final 필드들을 자동으로 생성자 주입해 주는 Lombok 어노테이션 코드 추가
public class PetController {

    private final PetService petService; // 반려견 목록 때문에 추가함
    // PetService를 필드로 선언. @RequiredArgsConstructor가 이 필드를 자동으로 생성자 주입해 줌
    // 이게 없으면 아래에서 petService.getMyPets()를 쓸 수가 없음

    private final AtomicLong SEQUENCE = new AtomicLong(1L);

    // 반려견 목록에 쓰일 코드로 아래 수정 (더미데이터였음)
    @GetMapping
    public ResponseEntity<SuccessResponseDto<List<PetResponse>>> getPetsList(
        @AuthenticationPrincipal String email) {
        // JWT 토큰에서 꺼낸 로그인한 사람의 email이 자동으로 여기 들어옴

        List<PetResponse> pets = petService.getMyPets(email);
        // 더미 데이터 대신, 실제로 DB에서 이 사람의 반려견 목록을 조회함

        return ResponseEntity.ok(SuccessResponseDto.success("반려견 목록을 조회했습니다.", pets));
        // dummy 대신 pets를 리턴하도록 변수명만 바꿈
    }

    // 반려견 추가에 쓰일 코드로 아래 수정
    @PostMapping
    public ResponseEntity<SuccessResponseDto<List<PetResponse>>> registerPets(

        @AuthenticationPrincipal String email, // 지금 로그인한 사람의 이메일을 자동으로 이 email 변수에 넣어달라는 코드 추가
        @Valid @RequestBody List<PetRegisterRequest> requests){

        List<PetResponse> responses = petService.registerPets(email, requests);
        // 더미로 SEQUENCE 번호 매기던 로직 대신 Service에게 email과 요청 리스트를 넘겨서 진짜로 DB에 저장시킴

        return ResponseEntity.ok(SuccessResponseDto.success("반려견 정보가 등록되었습니다.", responses));
    }

    @PatchMapping("/{petId}")
    public ResponseEntity<SuccessResponseDto<PetResponse>> updatePet(@PathVariable Long petId,
                                                                     @RequestBody PetUpdateRequest request){

        PetResponse response = PetResponse.builder()
            .petId(petId)
            .name(request.getName() != null ? request.getName() : "초코")
            .breed(request.getBreed() != null ? request.getBreed().name() : "MALTESE")  // ← .name() 추가
            .size(request.getSize() != null ? request.getSize().name() : "SMALL")  // ← .name() 추가
            .imageUrl(request.getImageUrl() != null ? request.getImageUrl() : "https://placehold.co/400x400")
            .build();

        return ResponseEntity.ok(SuccessResponseDto.success("반려견 정보가 수정되었습니다.", response));
    }


    @DeleteMapping("/{petId}")
    public ResponseEntity<SuccessResponseDto<Void>> deletePet(@PathVariable Long petId){
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("반려견 정보가 삭제되었습니다."));
    }
}
