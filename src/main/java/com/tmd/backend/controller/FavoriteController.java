package com.tmd.backend.controller;

import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.favorite.FavoriteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


// 즐겨찾기 목록 기능을 위해 추가
import com.tmd.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Slf4j
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
// 즐겨찾기 목록 기능을 위해 추가: 아래 favoriteService를 자동 생성자 주입 가능하게 함


public class FavoriteController {

    private final FavoriteService favoriteService;
    // 즐겨찾기 목록 기능을 위해 추가: Service를 주입받음. 실제 로직 처리는 얘한테 다 시킴

    @GetMapping
    public ResponseEntity<SuccessResponseDto<PageResponse<FavoriteResponse>>> getFavorites(
        @AuthenticationPrincipal String email,
        // 즐겨찾기 목록 기능을 위해 추가: JWT 토큰에서 꺼낸 로그인한 사람의 email이 자동으로 들어옴

        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ){
        log.info("즐겨찾기 목록 조회 요청: page={}, size={}", page, size);

        PageResponse<FavoriteResponse> response = favoriteService.getMyFavorites(email, page, size);
        // 즐겨찾기 목록 기능을 위해 수정: 더미 데이터(List.of(...)) 대신 Service에게 email, page, size를 넘겨서 실제 DB 조회 결과를 받아옴

        return ResponseEntity.ok(SuccessResponseDto.success("즐겨찾기 목록을 조회했습니다.", response));
    }

    // 즐겨찾기 여부 조회 기능을 위해 추가
    @GetMapping("/{placeId}")
    // GET 방식으로 "/favorites/{placeId}" 요청이 오면 아래 메서드가 실행됨
    // {placeId} 자리에 실제 숫자(예: /favorites/10)가 들어오면 그 값을 받게 됨
    public ResponseEntity<SuccessResponseDto<Boolean>> getFavoriteStatus(
        // 이 장소가 즐겨찾기 되어있는지 true/false를 응답으로 돌려주는 메서드

        @AuthenticationPrincipal String email,  // JWT 토큰에서 꺼낸 로그인한 사람의 email이 자동으로 들어옴
        @PathVariable Long placeId  // URL 경로에 있는 {placeId} 값을 그대로 받아옴, 예: /favorites/10 요청이면 placeId = 10L
    ) {
        log.info("즐겨찾기 여부 조회 요청: placeId={}", placeId);  // 요청 들어온 것을 로그로 남김

        boolean isFavorite = favoriteService.isFavorite(email, placeId);
        // Service에게 email과 placeId를 넘겨서 이 사람이 이 장소를 즐겨찾기했는지 확인시킴 (결과는 true 또는 false로 돌아옴)

        return ResponseEntity.ok(SuccessResponseDto.success("즐겨찾기 여부를 조회했습니다.", isFavorite));
        // 200 OK 상태코드와 함께 true/false 값을 공통 응답 형식(SuccessResponseDto)으로 감싸서 리턴
    }


    // 즐겨찾기 추가 - FavoriteController.java의 addFavorite 더미 코드를 진짜 로직으로 교체함
    @PostMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<Void>> addFavorite(
        @AuthenticationPrincipal String email,  // JWT 토큰에서 꺼낸 로그인한 사람의 email이 자동으로 들어옴

        @PathVariable Long placeId  // URL 경로의 {placeId} 값을 받아옴
    ){
        log.info("즐겨찾기 추가 요청: placeId={}", placeId);

        favoriteService.addFavorite(email, placeId);
        // Service에게 email과 placeId를 넘겨서 실제로 DB에 즐겨찾기를 등록시킴

        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("즐겨찾기에 추가되었습니다."));
    }

    // 즐겨찾기 삭제 - FavoriteController.java의 deleteFavorite 더미 코드를 진짜 로직으로 교체
    @DeleteMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<Void>> deleteFavorite(
        @AuthenticationPrincipal String email,  // JWT 토큰에서 꺼낸 로그인한 사람의 email이 자동으로 들어옴

        @PathVariable Long placeId  // URL 경로의 {placeId} 값을 받아옴
    ){
        log.info("즐겨찾기 삭제 요청: placeId={}", placeId);

        favoriteService.deleteFavorite(email, placeId);
        // Service에게 email과 placeId를 넘겨서 실제로 DB에서 즐겨찾기를 삭제시킴

        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("즐겨찾기가 삭제되었습니다."));
    }
}
