package com.tmd.backend.dto.response.pet;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetResponse {
    private Long petId;
    private String name;
    private String breed;
    private Double weight;
    private String imageUrl;

    private boolean hasMuzzle;
    private boolean hasLeash;
    private boolean hasCarrier;

    @Builder
    public PetResponse(Long petId, String name, String breed, Double weight, String imageUrl,
                       boolean hasMuzzle, boolean hasLeash, boolean hasCarrier) {
        this.petId = petId;
        this.name = name;
        this.breed = breed;
        this.weight = weight;
        this.imageUrl = imageUrl;
        this.hasMuzzle = hasMuzzle;
        this.hasLeash = hasLeash;
        this.hasCarrier = hasCarrier;
    }
}
