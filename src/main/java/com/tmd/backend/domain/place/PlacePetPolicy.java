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
    private AccessScope accessScope;

    private Boolean allBreedsAllowed;
    private Boolean dangerousBreedAllowed;
    private Boolean dangerousBreedAllowedCondition;

    private Double maxWeightKg;

    @Enumerated(EnumType.STRING)
    private WeightLimitType weightLimitType;

    private Boolean leashRequired;
    private Boolean muzzleRequired;
    private Boolean kennelRequired;
    private Boolean advanceInquiryRequired;
    private Integer maxPetCount;

    @Builder
    private PlacePetPolicy(Place place, AccessScope accessScope, Boolean allBreedsAllowed, Boolean dangerousBreedAllowed, Boolean dangerousBreedAllowedCondition, Double maxWeightKg, WeightLimitType weightLimitType, Boolean leashRequired, Boolean muzzleRequired, Boolean kennelRequired, Boolean advanceInquiryRequired, Integer maxPetCount) {
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
            .build();
    }
}
