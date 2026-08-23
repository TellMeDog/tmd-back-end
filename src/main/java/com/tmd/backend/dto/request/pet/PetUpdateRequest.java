package com.tmd.backend.dto.request.pet;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 강아지 사이즈/종 enum 쓰려고 import 추가
import com.tmd.backend.domain.pet.Breed;
import com.tmd.backend.domain.pet.PetSize;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetUpdateRequest {
    private Long petId;
    private String name;
    private Breed breed; // enum
    private PetSize size; // enum
    private String imageUrl;
}
