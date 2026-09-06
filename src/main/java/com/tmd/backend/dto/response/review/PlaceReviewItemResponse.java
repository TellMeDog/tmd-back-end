package com.tmd.backend.dto.response.review;

import com.tmd.backend.domain.review.FeedbackType;
import com.tmd.backend.domain.review.MismatchReason;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class PlaceReviewItemResponse {
    private Long reviewId;
    private String writer;
    private FeedbackType feedbackType;
    private List<MismatchReason> mismatchReasons;
    private String etcReason;
    private int rating;
    private String content;
    private String imageKey;
    private String createdAt;
}
