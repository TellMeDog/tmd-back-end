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
    private String breed;  // [수정 없음] breed는 이미 String이었음, 그대로 유지
    private Double size;  // [수정] String → Double로 변경 (숫자값 그대로 응답)
    private String imageUrl;

    private boolean hasMuzzle;
    private boolean hasLeash;
    private boolean hasCarrier;
    // [수정] isVaccinated 필드 제거

    @Builder
    public PetResponse(Long petId, String name, String breed, Double size, String imageUrl,
                       boolean hasMuzzle, boolean hasLeash, boolean hasCarrier) {
        // [수정] 매개변수: String size → Double size, boolean isVaccinated 제거

        this.petId = petId;
        this.name = name;
        this.breed = breed;
        this.size = size;
        this.imageUrl = imageUrl;
        this.hasMuzzle = hasMuzzle;
        this.hasLeash = hasLeash;
        this.hasCarrier = hasCarrier;
    }
}
