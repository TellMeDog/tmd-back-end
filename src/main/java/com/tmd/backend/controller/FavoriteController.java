package com.tmd.backend.controller;

import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.favorite.FavoriteResponse;
import com.tmd.backend.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "Favorite", description = "즐겨찾기 API")
@RestController
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {
    private final FavoriteService favoriteService;

    @Operation(
        summary = "나의 즐겨찾기 목록 조회",
        description = "마이페이지에서 나의 즐겨찾기 목록을 클릭시 호출합니다."
    )
    @GetMapping
    public ResponseEntity<SuccessResponseDto<PageResponse<FavoriteResponse>>> getMyFavorites(
        @Parameter(description = "반려견 ID", example = "1") @RequestParam Long petId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        return ResponseEntity.ok(SuccessResponseDto.success(
            "즐겨찾기 목록을 조회했습니다.",
            favoriteService.getMyFavorites(email, petId, page, size)
        ));
    }

    @Operation(
        summary = "즐겨찾기 추가",
        description = "장소를 즐겨찾기에 추가합니다."
    )
    @PostMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<Void>> addFavorite(
        @Parameter(description = "장소 ID", example = "1") @PathVariable Long placeId,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        favoriteService.addFavorite(email, placeId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("즐겨찾기에 추가되었습니다."));
    }

    @Operation(
        summary = "즐겨찾기 삭제",
        description = "장소를 즐겨찾기에서 삭제합니다."
    )
    @DeleteMapping("/{placeId}")
    public ResponseEntity<SuccessResponseDto<Void>> deleteFavorite(
        @Parameter(description = "장소ID", example = "1") @PathVariable Long placeId,
        @AuthenticationPrincipal(expression = "username") String email
    ) {
        favoriteService.deleteFavorite(email, placeId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("즐겨찾기가 삭제되었습니다."));
    }
}
