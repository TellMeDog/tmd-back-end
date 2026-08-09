package com.tmd.backend.dto.request.pet;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetUpdateRequest {
    private Long petId;
    private String name;
    private String breed;
    private String size;
    private String imageUrl;

    @Builder
    public PetUpdateRequest(Long petId, String name, String breed, String size, String imageUrl) {
        this.petId=petId;
        this.name = name;
        this.breed = breed;
        this.size=size;
        this.imageUrl=imageUrl;
    }
}
