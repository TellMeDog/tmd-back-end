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

    @PostMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<Void>> addFavorite(@PathVariable Long placeId){
        log.info("즐겨찾기 추가 요청: placeId={}", placeId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("즐겨찾기에 추가되었습니다."));
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<Void>> deleteFavorite(@PathVariable Long placeId){
        log.info("즐겨찾기 삭제 요청: placeId={}", placeId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("즐겨찾기가 삭제되었습니다."));
    }
}
