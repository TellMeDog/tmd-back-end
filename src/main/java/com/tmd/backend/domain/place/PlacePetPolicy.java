package com.tmd.backend.domain.place;

import com.tmd.backend.ai.AccessScope;
import com.tmd.backend.ai.PetPolicyAnalysis;
import com.tmd.backend.ai.WeightLimitType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlacePetPolicy {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Enumerated(EnumType.STRING)
    private AccessScope accessScope; // 동반 가능 구역

    private Boolean allBreedsAllowed; // 동반 가능 견종
    private Boolean dangerousBreedAllowed; // 맹견 입장 가능 여부 (ex. 입마개 착용시 가능이면 True)
    private String dangerousBreedAllowedCondition; // 맹견 입장 가능 조건 (ex. MUZZLE)

    private Double maxWeightKg; // 입장 가능 최대 몸무게

    @Enumerated(EnumType.STRING)
    private WeightLimitType weightLimitType; // 미만, 이하, 알 수 없음

    private Boolean leashRequired; // 목줄 필수 여부
    private Boolean muzzleRequired; // 입마개 필수 여부
    private Boolean kennelRequired; // 이동장, 켄넬 필수 여부
    private Boolean advanceInquiryRequired; // 사전 문의 필수 여부
    private Integer maxPetCount; // 최대 허용 반려견 수

    private String defaultPolicy; // YELLOW OR NULL

    @Column(nullable = false, columnDefinition = "bit default 0")
    private Boolean reviewPending = false;

    @Builder
    private PlacePetPolicy(Place place, AccessScope accessScope, Boolean allBreedsAllowed, Boolean dangerousBreedAllowed, String dangerousBreedAllowedCondition, Double maxWeightKg, WeightLimitType weightLimitType, Boolean leashRequired, Boolean muzzleRequired, Boolean kennelRequired, Boolean advanceInquiryRequired, Integer maxPetCount, String defaultPolicy) {
        this.place = place;
        this.accessScope = accessScope;
        this.allBreedsAllowed = allBreedsAllowed;
        this.dangerousBreedAllowed = dangerousBreedAllowed;
        this.dangerousBreedAllowedCondition = dangerousBreedAllowedCondition;
        this.maxWeightKg = maxWeightKg;
        this.weightLimitType = weightLimitType;
        this.leashRequired = leashRequired;
        this.muzzleRequired = muzzleRequired;
        this.kennelRequired = kennelRequired;
        this.advanceInquiryRequired = advanceInquiryRequired;
        this.maxPetCount = maxPetCount;
        this.defaultPolicy=defaultPolicy;
    }

    public static PlacePetPolicy from(Place place, PetPolicyAnalysis analysis) {
        return PlacePetPolicy.builder()
            .place(place)
            .accessScope(analysis.accessScope())
            .allBreedsAllowed(analysis.allBreedsAllowed())
            .dangerousBreedAllowed(analysis.dangerousBreedAllowed())
            .dangerousBreedAllowedCondition(analysis.dangerousBreedAllowedCondition())
            .maxWeightKg(analysis.maxWeightKg())
            .weightLimitType(analysis.weightLimitType())
            .leashRequired(analysis.leashRequired())
            .muzzleRequired(analysis.muzzleRequired())
            .kennelRequired(analysis.kennelRequired())
            .advanceInquiryRequired(analysis.advanceInquiryRequired())
            .maxPetCount(analysis.maxPetCount())
            .defaultPolicy(analysis.defaultPolicy())
            .build();
    }

    public void update(PetPolicyAnalysis analysis) {
        this.accessScope = analysis.accessScope();
        this.allBreedsAllowed = analysis.allBreedsAllowed();
        this.dangerousBreedAllowed = analysis.dangerousBreedAllowed();
        this.dangerousBreedAllowedCondition = analysis.dangerousBreedAllowedCondition();
        this.maxWeightKg = analysis.maxWeightKg();
        this.weightLimitType = analysis.weightLimitType();
        this.leashRequired = analysis.leashRequired();
        this.muzzleRequired = analysis.muzzleRequired();
        this.kennelRequired = analysis.kennelRequired();
        this.advanceInquiryRequired = analysis.advanceInquiryRequired();
        this.maxPetCount = analysis.maxPetCount();
        this.defaultPolicy = analysis.defaultPolicy();
        this.reviewPending = false;
    }

    public void markReviewPending() {
        this.reviewPending = true;
    }
}
