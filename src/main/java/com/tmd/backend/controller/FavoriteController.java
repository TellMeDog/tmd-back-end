package com.tmd.backend.controller;

import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.favorite.FavoriteResponse;
import com.tmd.backend.service.FavoriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @GetMapping
    public ResponseEntity<SuccessResponseDto<PageResponse<FavoriteResponse>>> getFavorites(
        @RequestParam Long petId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "즐겨찾기 목록을 조회했습니다.",
            favoriteService.getMyFavorites(email, petId, page, size)
        ));
    }

    @GetMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<Boolean>> getFavoriteStatus(
        @PathVariable Long placeId,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "즐겨찾기 여부를 조회했습니다.",
            favoriteService.isFavorite(email, placeId)
        ));
    }

    @PostMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<Void>> addFavorite(
        @PathVariable Long placeId,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        favoriteService.addFavorite(email, placeId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("즐겨찾기에 추가되었습니다."));
    }

    @DeleteMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<Void>> deleteFavorite(
        @PathVariable Long placeId,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        favoriteService.deleteFavorite(email, placeId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("즐겨찾기가 삭제되었습니다."));
    }
}
