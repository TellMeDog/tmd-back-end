package com.tmd.backend.dto.response.review;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewSummaryResponse {
    private Long reviewId;
    private String writer;
    private String feedbackType;
    private List<String> mismatchReasons;
    private Integer rating;
    private String content;
    private String imageUrl;
    private String createdAt;
}
