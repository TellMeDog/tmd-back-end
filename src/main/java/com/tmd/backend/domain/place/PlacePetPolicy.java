package com.tmd.backend.domain.place;

import com.tmd.backend.ai.AccessScope;
import com.tmd.backend.ai.PetPolicyAnalysis;
import com.tmd.backend.ai.WeightLimitType;
import jakarta.persistence.*;
import lombok.AccessLevel;
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

    private Boolean petAllowed;
    private Boolean allBreedsAllowed;
    private Boolean dangerousBreedAllowed;
    private Boolean dangerousBreedMuzzleRequired;

    private Double maxWeightKg;
    @Enumerated(EnumType.STRING)
    private WeightLimitType weightLimitType;

    private Boolean leashRequired;
    private Boolean muzzleRequired;
    private Boolean kennelRequired;
    private Boolean strollerAllowed;
    private Boolean vaccinationRequired;
    private Boolean advanceInquiryRequired;
    private Integer maxPetCount;

    @Enumerated(EnumType.STRING)
    private AccessScope accessScope;
    private String accessAreaDescription;

    @Column(columnDefinition = "TEXT")
    private String exceptions;

    public static PlacePetPolicy from(Place place, PetPolicyAnalysis analysis) {
        PlacePetPolicy p = new PlacePetPolicy();
        p.place = place;
        p.petAllowed = analysis.petAllowed();
        p.allBreedsAllowed = analysis.allBreedsAllowed();
        p.dangerousBreedAllowed = analysis.dangerousBreedAllowed();
        p.dangerousBreedMuzzleRequired = analysis.dangerousBreedMuzzleRequired();
        p.maxWeightKg = analysis.maxWeightKg();
        p.weightLimitType = analysis.weightLimitType();
        p.leashRequired = analysis.leashRequired();
        p.muzzleRequired = analysis.muzzleRequired();
        p.kennelRequired = analysis.kennelRequired();
        p.strollerAllowed = analysis.strollerAllowed();
        p.vaccinationRequired = analysis.vaccinationRequired();
        p.advanceInquiryRequired = analysis.advanceInquiryRequired();
        p.maxPetCount = analysis.maxPetCount();
        p.accessScope = analysis.accessScope();
        p.accessAreaDescription = analysis.accessAreaDescription();
        p.exceptions = analysis.exceptions();
        return p;
    }
}
