package com.tmd.backend.service;

import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.place.PlacePetPolicy;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class MarkerColorService {

    private static final Set<PetBreed> dangerousBreed = Set.of(PetBreed.DALMATIAN);

    public String calculateMarkerColor(PlacePetPolicy policy, Pet pet) {
        //TODO: 마커 색깔 판단 로직 구현
        if (policy == null) {
            return "GREY";
        }
        if ("YELLOW".equals(policy.getDefaultPolicy())) {
            return "YELLOW";
        }
        return "GREEN";
    }
}
