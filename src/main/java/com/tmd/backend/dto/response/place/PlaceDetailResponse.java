package com.tmd.backend.dto.response.place;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceDetailResponse {
    private Long placeId;
    private String contentId;
    private String zipCode;
    private String addr1;
    private String addr2;
    private String title;
    private double mapX;
    private double mapY;
    private String firstImage;
    private String firstImage2;
    private String dist; // 검색 좌표로부터 거리
    private String modifiedTime; // 최근 수정일
    private String markerColor;
    private PetPolicyInfo petPolicyInfo;
    private boolean isFavorite;
    private double averageRating;
    private VisitStats visitStats;

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
    public static class VisitStats {
        private int enteredCount; // ex. 안내대로 입장했어요 22명 (feedbackType == "ENTERED")
        private int mismatchedCount; // ex. 안내된 조건과 달랐어요. 12명 (feedbackType == "MISMATCHED_INFO")
        private int deniedCount; // ex. 입장이 불가능했어요. 1명 (feedbackType == "DENIED")
        private String lastReportedAt; // ex. 마지막 제보 2026-08-22
    }
}
