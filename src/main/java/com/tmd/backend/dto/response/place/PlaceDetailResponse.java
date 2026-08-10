package com.tmd.backend.dto.response.place;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceDetailResponse {
    private Long placeId;
    private String contentId;
    private String title;
    private String addr;
    private double mapX;
    private double mapY;
    private String markerColor;
    private Checklist checklist;
    private PetPolicyRawText petPolicyRawText;
    private boolean isFavorite;
    private double averageRating;
    private VisitStats visitStats;

    @Getter
    @Builder
    public static class Checklist {
        private boolean leashRequired;
        private boolean muzzleRequired;
        private boolean wasteBagRequired;
    }

    @Getter
    @Builder
    public static class PetPolicyRawText {
        private String acmpyTypeCd;
        private String acmpyNeedMtr;
        private String etcAcmpyInfo;
    }

    @Getter
    @Builder
    public static class VisitStats {
        private int enteredCount;
        private int mismatchedCount;
        private int deniedCount;
        private String lastReportedAt;
    }
}
