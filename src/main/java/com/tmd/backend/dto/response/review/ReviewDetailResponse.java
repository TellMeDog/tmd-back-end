package com.tmd.backend.dto.response.review;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ReviewDetailResponse {
    private Long reviewId;
    private Long placeId;
    private String placeTitle;
    private Long petId;
    private String petName;
    private String feedbackType;
    private List<String> mismatchReasons;
    private Integer rating;
    private String content;
    private String imageUrl;
    private String createdAt;
}
