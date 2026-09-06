package com.tmd.backend.controller;

import com.tmd.backend.dto.request.review.ReviewCreateRequest;
import com.tmd.backend.dto.request.review.ReviewUpdateRequest;
import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.dto.response.review.MyReviewListResponse;
import com.tmd.backend.dto.response.review.PlaceReviewItemResponse;
import com.tmd.backend.dto.response.review.ReviewDetailResponse;
import com.tmd.backend.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;

    @PostMapping("/places/{placeId}/reviews")
    public ResponseEntity<SuccessResponseDto<Void>> createReview(
        @PathVariable Long placeId,
        @RequestBody @Valid ReviewCreateRequest request,
        @AuthenticationPrincipal(expression = "username") String email) {

        reviewService.createReview(email, placeId, request);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("리뷰가 등록되었습니다."));
    }

    @GetMapping("/places/{placeId}/reviews")
    public ResponseEntity<SuccessResponseDto<PageResponse<PlaceReviewItemResponse>>> getPlaceReviews(
        @PathVariable Long placeId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "latest") String sort) {

        return ResponseEntity.ok(SuccessResponseDto.success(
            "리뷰 목록을 조회했습니다.",
            reviewService.getPlaceReviewList(placeId, page, size, sort)
        ));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponseDto<Void>> updateReview(
        @PathVariable Long reviewId,
        @RequestBody @Valid ReviewUpdateRequest request,
        @AuthenticationPrincipal(expression = "username") String email) {

        reviewService.updateReview(email, reviewId, request);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("리뷰가 수정되었습니다."));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponseDto<Void>> deleteReview(
        @PathVariable Long reviewId,
        @AuthenticationPrincipal(expression = "username") String email) {

        reviewService.deleteReview(email, reviewId);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("리뷰가 삭제되었습니다."));
    }

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponseDto<ReviewDetailResponse>> getMyReviewDetail(
        @PathVariable Long reviewId,
        @AuthenticationPrincipal(expression = "username") String email) {

        return ResponseEntity.ok(SuccessResponseDto.success(
            "리뷰 상세 정보를 조회했습니다.",
            reviewService.getMyReview(reviewId, email)
        ));
    }

    @GetMapping("/reviews")
    public ResponseEntity<SuccessResponseDto<PageResponse<MyReviewListResponse>>> getMyReviews(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @AuthenticationPrincipal(expression = "username") String email) {

        return ResponseEntity.ok(SuccessResponseDto.success(
            "내 리뷰 목록을 조회했습니다.",
            reviewService.getMyReviewList(email, page, size)
        ));
    }
}
