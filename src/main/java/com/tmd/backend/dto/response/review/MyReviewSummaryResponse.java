package com.tmd.backend.dto.response.review;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyReviewSummaryResponse {
    private Long reviewId;
    private Long placeId;
    private String placeTitle;
    private String placeAddr;
    private String imageUrl;
    private String markerColor;
}
