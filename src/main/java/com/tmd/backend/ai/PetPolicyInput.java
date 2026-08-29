package com.tmd.backend.ai;

import com.tmd.backend.external.TourApiPetInfoItem;

public record PetPolicyInput(String accidentRisk, String accompanyType, String possibleCapacity, String requiredItems, String additionalInfo) {

    public PetPolicyInput toPolicyInput(TourApiPetInfoItem item){
        return new PetPolicyInput(
            item.getRelaAcdntRiskMtr(),
            item.getAcmpyTypeCd(),
            item.getAcmpyPsblCpam(),
            item.getAcmpyNeedMtr(),
            item.getEtcAcmpyInfo()
        );
    }
}

