package com.tmd.backend.dto.request.pet;

import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetUpdateRequest {
    private String name;
    private String breed;

    @Positive(message = "몸무게는 0보다 커야 합니다.")
    private Double weight;

    private UUID imageUploadId;
    private boolean removeImage;
    private Boolean hasMuzzle;
    private Boolean hasLeash;
    private Boolean hasCarrier;
}
