package com.tmd.backend.controller;

import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.favorite.FavoriteResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/favorites")
public class FavoriteController {

    @GetMapping
    public ResponseEntity<SuccessResponseDto<PageResponse<FavoriteResponse>>> getFavorites(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ){
        log.info("즐겨찾기 목록 조회 요청: page={}, size={}", page, size);

        List<FavoriteResponse> dummy = List.of(
            FavoriteResponse.builder()
                .placeId(10L)
                .contentId("111111")
                .thumbnailUrl("https://placehold.co/400x400")
                .addr("서울 용산구 ..")
                .title("용산공원")
                .markerColor("GREEN")
                .build()
        );
        PageResponse<FavoriteResponse> response = new PageResponse<>(dummy, dummy.size(), 1);

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
