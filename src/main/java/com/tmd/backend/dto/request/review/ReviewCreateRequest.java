package com.tmd.backend.dto.request.review;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewCreateRequest {

    private Long petId;

    @NotNull(message = "방문 결과를 선택해주세요.")
    private String feedbackType;

    private List<String> mismatchReasons;

    private Integer rating;

    private String content;

    private String imageUrl;

}
