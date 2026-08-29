package com.tmd.backend.service;

import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.pet.PetSize;
import com.tmd.backend.domain.place.PlacePetInfo;
import com.tmd.backend.dto.response.place.PlaceDetailResponse;
import org.springframework.stereotype.Service;

@Service
public class MarkerColorService {

    public String calculateMarkerColor(PlacePetInfo info, Pet pet) {
        //TODO: 마커 색깔 판단 로직 구현
        return "GREEN";
    }
}
