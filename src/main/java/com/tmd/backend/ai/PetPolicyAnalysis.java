package com.tmd.backend.ai;

import java.util.List;

public record PetPolicyAnalysis(
    AccessScope accessScope,
    List<PolicyCondition> conditions,
    Integer maxPetsPerPerson,
    WeightLimit weightLimit,
    List<String> exceptions,
    String summary
) {
}
