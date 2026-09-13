package com.tmd.backend.ai;

public record PetPolicyAnalysis(
    Long placeId,

    AccessScope accessScope, // 전 구역, 일부 구역, 불가, 정보없음
    Boolean allBreedsAllowed, // 모든 견종 가능
    Boolean dangerousBreedAllowed, // 맹견 입장 가능
    String dangerousBreedAllowedCondition, // 맹견 입장 가능 조건

    Double maxWeightKg, // 최대 무게
    WeightLimitType weightLimitType, // 이하, 미만, 정보없음

    Boolean leashRequired, // 목줄 필수 여부
    Boolean muzzleRequired, // 입마개 필수 여부
    Boolean kennelRequired, // 이동장 필수 여부

    Boolean advanceInquiryRequired, // 사전 문의 필수 여부

    Integer maxPetCount, // 최대 동반 가능 마리 수

    String defaultPolicy // YELLOW or NULL
) {
}
