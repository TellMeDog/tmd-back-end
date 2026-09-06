package com.tmd.backend.ai;

public record PetPolicyAnalysis(
    Long placeId,

    Boolean petAllowed,
    Boolean allBreedsAllowed,
    Boolean dangerousBreedAllowed,
    Boolean dangerousBreedMuzzleRequired,

    Double maxWeightKg,
    WeightLimitType weightLimitType,

    Boolean leashRequired,
    Boolean muzzleRequired,
    Boolean kennelRequired,
    Boolean strollerAllowed,

    Boolean vaccinationRequired,
    Boolean advanceInquiryRequired,

    Integer maxPetCount,

    AccessScope accessScope,
    String accessAreaDescription,

    String exceptions
) {
}
