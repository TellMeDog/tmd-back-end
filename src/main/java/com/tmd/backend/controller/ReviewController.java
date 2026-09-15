package com.tmd.backend.controller;

import com.tmd.backend.dto.request.review.ReviewCreateRequest;
import com.tmd.backend.dto.request.review.ReviewUpdateRequest;
import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.review.MyReviewListResponse;
import com.tmd.backend.dto.response.review.ReviewDetailResponse;
import com.tmd.backend.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "Review", description = "리뷰 API")
@RestController
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @Operation(
        summary = "리뷰 작성",
        description = "장소에 대한 리뷰를 작성합니다.")
    @PostMapping("/reviews/{placeId}")
    public ResponseEntity<SuccessResponseDto<Void>> createReview(
        @Parameter(description = "장소 ID", example = "1") @PathVariable Long placeId,
        @RequestBody @Valid ReviewCreateRequest request,
        @AuthenticationPrincipal String email) {

        reviewService.createReview(email, placeId, request);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("리뷰가 등록되었습니다."));
    }

    @Operation(
        summary = "리뷰 수정",
        description = "장소에 대한 리뷰를 수정합니다.")
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponseDto<Void>> updateReview(
        @PathVariable Long reviewId,
        @RequestBody @Valid ReviewUpdateRequest request,
        @AuthenticationPrincipal String email) {

        reviewService.updateReview(email, reviewId, request);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("리뷰가 수정되었습니다."));
    }

    @Operation(
        summary = "리뷰 삭제",
        description = "장소에 대한 리뷰를 삭제합니다.")
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponseDto<Void>> deleteReview(
        @PathVariable Long reviewId,
        @AuthenticationPrincipal String email) {

        reviewService.deleteReview(email, reviewId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("리뷰가 삭제되었습니다."));
    }

    @Operation(
        summary = "리뷰 리스트 조회 (무한 스크롤링, 페이지)",
        description = "장소 클릭 이후 추가로 리뷰를 요청할때 사용하는 API")
    @GetMapping("/reviews/{placeId}")
    public ResponseEntity<SuccessResponseDto<PageResponse<ReviewDetailResponse>>> getPlaceReviews(
        @PathVariable Long placeId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "latest") String sort,
        @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(SuccessResponseDto.success(
            "리뷰 목록을 조회했습니다.",
            reviewService.getPlaceReviewList(placeId, email, page, size, sort)
        ));
    }

    @Operation(
        summary = "내 리뷰 목록 조회",
        description = "마이페이지에서 내 리뷰 목록을 조회합니다.")
    @GetMapping("/reviews/mypage/{petId}")
    public ResponseEntity<SuccessResponseDto<PageResponse<MyReviewListResponse>>> getMyReviews(
        @Parameter(description = "반려견 ID", example = "1") @PathVariable Long petId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal String email) {

        return ResponseEntity.ok(SuccessResponseDto.success(
            "내 리뷰 목록을 조회했습니다.",
            reviewService.getMyReviewListInMyPage(petId, email, page, size)
        ));
    }
}
