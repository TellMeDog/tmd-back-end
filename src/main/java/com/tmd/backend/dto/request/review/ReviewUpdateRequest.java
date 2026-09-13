package com.tmd.backend.dto.request.review;

import com.tmd.backend.domain.review.FeedbackType;
import com.tmd.backend.domain.review.MismatchReason;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewUpdateRequest {
    @NotNull(message = "방문 결과를 선택해주세요.")
    private FeedbackType feedbackType;
    private List<MismatchReason> mismatchReasons;
    private String etcReason;

    @NotNull(message = "별점을 선택해주세요.")
    @Min(value = 1, message = "별점은 1 이상이어야 합니다.")
    @Max(value = 5, message = "별점은 5 이하여야 합니다.")
    private Integer rating;

    private String content;
    private UUID imageUploadId;
    private boolean removeImage;
}
