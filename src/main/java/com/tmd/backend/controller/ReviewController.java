package com.tmd.backend.controller;

import com.tmd.backend.dto.request.review.ReviewCreateRequest;
import com.tmd.backend.dto.response.*;
import com.tmd.backend.dto.response.review.MyReviewSummaryResponse;
import com.tmd.backend.dto.response.review.ReviewDetailResponse;
import com.tmd.backend.dto.response.review.ReviewSummaryResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
public class ReviewController {

    @PostMapping("/places/{placeId}/reviews")
    public ResponseEntity<SuccessResponseDto<Void>> createReview(
        @PathVariable Long placeId,
        @RequestBody @Valid ReviewCreateRequest request) {

        log.info("리뷰 작성 요청: placeId={}, feedbackType={}", placeId, request.getFeedbackType());
        // TODO: feedbackType이 MISMATCHED_INFO가 아닌데 mismatchReasons가 있으면 INVALID_REVIEW_REQUEST 예외 처리 필요

        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("리뷰가 등록되었습니다."));
    }

    @GetMapping("/places/{placeId}/reviews")
    public ResponseEntity<SuccessResponseDto<PageResponse<ReviewSummaryResponse>>> getReviews(
        @PathVariable Long placeId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(defaultValue = "latest") String sort) {

        log.info("리뷰 목록 조회: placeId={}, sort={}", placeId, sort);

        List<ReviewSummaryResponse> dummy = List.of(
            ReviewSummaryResponse.builder()
                .reviewId(5L)
                .writer("us***@gmail.com")
                .feedbackType("MISMATCHED_INFO")
                .mismatchReasons(List.of("LEASH_REQUIRED", "MUZZLE_REQUIRED"))
                .rating(3)
                .content("목줄 말고 입마개도 추가로 요구했어요")
                .imageUrl("https://placehold.co/400x400")
                .createdAt("2026-08-01T12:00:00")
                .build()
        );

        PageResponse<ReviewSummaryResponse> response = new PageResponse<>(dummy, dummy.size(), 1);

        return ResponseEntity.ok(SuccessResponseDto.success("리뷰 목록을 조회했습니다.", response));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponseDto<Void>> deleteReview(@PathVariable Long reviewId) {
        log.info("리뷰 삭제 요청: reviewId={}", reviewId);
        // TODO: 본인 작성 리뷰가 아니면 FORBIDDEN 예외 처리 필요

        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("리뷰가 삭제되었습니다."));
    }

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<SuccessResponseDto<ReviewDetailResponse>> getReviewDetail(
        @PathVariable Long reviewId) {

        log.info("리뷰 상세 조회: reviewId={}", reviewId);

        ReviewDetailResponse response = ReviewDetailResponse.builder()
            .reviewId(reviewId)
            .placeId(12L)
            .placeTitle("OO 애견카페")
            .petId(1L)
            .petName("초코")
            .feedbackType("MISMATCHED_INFO")
            .mismatchReasons(List.of("LEASH_REQUIRED", "MUZZLE_REQUIRED"))
            .rating(3)
            .content("목줄 말고 입마개도 추가로 요구했어요")
            .imageUrl("https://placehold.co/400x400")
            .createdAt("2026-08-01T12:00:00")
            .build();

        return ResponseEntity.ok(SuccessResponseDto.success("리뷰 상세 정보를 조회했습니다.", response));
    }
    @GetMapping("/reviews")
    public ResponseEntity<SuccessResponseDto<PageResponse<MyReviewSummaryResponse>>> getMyReviews(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size) {

        log.info("내 리뷰 목록 조회 요청: page={}, size={}", page, size);

        List<MyReviewSummaryResponse> dummy = List.of(
            MyReviewSummaryResponse.builder()
                .reviewId(5L)
                .placeId(12L)
                .placeTitle("OO 애견카페")
                .placeAddr("서울 강남구 ...")
                .imageUrl("https://placehold.co/400x400")
                .markerColor("ORANGE")
                .build()
        );

        PageResponse<MyReviewSummaryResponse> response = new PageResponse<>(dummy, dummy.size(), 1);

        return ResponseEntity.ok(SuccessResponseDto.success("내 리뷰 목록을 조회했습니다.", response));
    }
}
