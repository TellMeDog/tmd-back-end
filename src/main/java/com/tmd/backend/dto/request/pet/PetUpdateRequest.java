package com.tmd.backend.dto.request.pet;

import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.pet.PetSize;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetUpdateRequest {
    private Long petId;
    private String name;
    private String breed;  // [수정] PetBreed(Enum) → String
    private Double size;  // [수정] PetSize(Enum) → Double
    private String imageUrl;
    private Boolean hasMuzzle;
    private Boolean hasLeash;
    private Boolean hasCarrier;
    // [수정] isVaccinated 필드 제거
}
