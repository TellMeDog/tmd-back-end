package com.tmd.backend.service;

import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.PlacePetPolicy;
import org.springframework.stereotype.Service;

@Service
public class MarkerColorService {

    public String calculateMarkerColor(PlacePetPolicy policy, Pet pet) {
        //TODO: 마커 색깔 판단 로직 구현
        /*if(info.getStatus().equals(PetInfoStatus.NO_DATA)){
            return "GREY";
        }*/
        return "GREEN";
    }
}
