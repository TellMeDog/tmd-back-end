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
}
