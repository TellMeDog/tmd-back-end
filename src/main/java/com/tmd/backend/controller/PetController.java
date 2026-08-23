// 프론트에서 오는 반려견 관련 요청(조회/등록/수정/삭제 등)을 받아서 실제 처리는 PetService에게 시키고, 그 결과를 정해진 응답 형식으로 포장해서 돌려주는 파일
// GET /pets, POST /pets, PATCH /pets/{petId}, DELETE /pets/{petId} 등의 API 입구 역할을 담당


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
    public ResponseEntity<SuccessResponseDto<PetResponse>> updatePet(
        @AuthenticationPrincipal String email,  // 반려견 수정 기능 - 소유권 검증을 위해 누가 요청했는지 알아야 함
        @PathVariable Long petId,
        @RequestBody PetUpdateRequest request){

        PetResponse response = petService.updatePet(email, petId, request);  // 더미 데이터 대신 반려견 수정 기능에 맞게 교체

        return ResponseEntity.ok(SuccessResponseDto.success("반려견 정보가 수정되었습니다.", response));
    }


    // 반려견 삭제에 쓰일 코드로 아래 수정
    @DeleteMapping("/{petId}")
    public ResponseEntity<SuccessResponseDto<Void>> deletePet(
        @AuthenticationPrincipal String email,  // 소유권 검증을 위해 필요
        @PathVariable Long petId){
        petService.deletePet(email, petId);  // 실제 삭제 로직 호출

        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("반려견 정보가 삭제되었습니다."));
    }
}
