package com.tmd.backend.dto.response.place;

import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.review.ReviewDetailResponse;
import lombok.Builder;
import lombok.Getter;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@Getter
@Builder
public class PlaceDetailResponse {
    private PlaceMarkerResponse placeMarkerResponse;
    private String zipCode;
    private String addr1;
    private String addr2;
    private String firstImage2;
    private String modifiedTime; // 최근 수정일
    private String placeType;
    private String tel;
    private String businessStatus;
    private PetPolicyInfo petPolicyInfo;
    private VisitStats visitStats;
    private List<ReviewDetailResponse> myReviews;
    private PageResponse<ReviewDetailResponse> reviews;

    @Getter
    @Builder
    public static class PetPolicyInfo {
        private String acmpyTypeCd;
        private String acmpyPsblCpam;
        private String acmpyNeedMtr;
        private String relaAcdntRiskMtr;
        private String relaPosesFclty;
        private String relaFrnshPrdlst;
        private String relaPurcPrdlst;
        private String relaRntlPrdlst;
        private String etcAcmpyInfo;
    }

    @Getter
    @Builder
    public static class VisitStats implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;

        private long enteredCount; // ex. 안내대로 입장했어요 22명 (feedbackType == "ENTERED")
        private long mismatchedCount; // ex. 안내된 조건과 달랐어요. 12명 (feedbackType == "MISMATCHED_INFO")
        private long deniedCount; // ex. 입장이 불가능했어요. 1명 (feedbackType == "DENIED")
        private String lastReportedAt; // ex. 마지막 제보 2026-08-22
        private List<String> topBreeds; // ex. 말티즈가 제일 많이 다녀갔어요!
    }
}
