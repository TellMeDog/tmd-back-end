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
    private String size;
    private String imageUrl;

    private boolean hasMuzzle;
    private boolean hasLeash;
    private boolean hasCarrier;
    private boolean isVaccinated;

    @Builder
    public PetResponse(Long petId, String name, String breed, String size, String imageUrl,
                       boolean hasMuzzle, boolean hasLeash, boolean hasCarrier, boolean isVaccinated) {
        this.petId = petId;
        this.name = name;
        this.breed = breed;
        this.size = size;
        this.imageUrl = imageUrl;
        this.hasMuzzle = hasMuzzle;
        this.hasLeash = hasLeash;
        this.hasCarrier = hasCarrier;
        this.isVaccinated = isVaccinated;
    }
}
