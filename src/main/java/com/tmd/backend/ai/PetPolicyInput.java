package com.tmd.backend.ai;

import com.tmd.backend.domain.place.PlacePetInfo;

public record PetPolicyInput(
    Long placeId,
    String accidentRisk,
    String possibleCapacity,
    String requiredItems,
    String additionalInfo
) {
    public static PetPolicyInput from(PlacePetInfo info) {
        return new PetPolicyInput(
            info.getPlace().getId(),
            info.getRelaAcdntRiskMtr(),
            info.getAcmpyPsblCpam(),
            info.getAcmpyNeedMtr(),
            info.getEtcAcmpyInfo()
        );
    }
}
