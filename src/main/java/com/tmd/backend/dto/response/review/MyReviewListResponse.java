package com.tmd.backend.dto.response.review;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MyReviewListResponse {
    private Long reviewId;
    private Long placeId;
    private String placeTitle;
    private String placeAddr1;
    private String placeAddr2;
    private String imageKey; // Open API 제공 썸네일
    private String markerColor;
}
