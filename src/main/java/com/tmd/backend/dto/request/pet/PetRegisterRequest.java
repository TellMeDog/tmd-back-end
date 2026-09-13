package com.tmd.backend.dto.request.pet;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetRegisterRequest {
    @NotBlank(message = "반려견 이름을 입력해주세요.")
    private String name;

    @NotBlank(message = "견종을 선택해주세요.")
    private String breed;

    @NotNull(message = "몸무게를 입력해주세요.")
    @Positive(message = "몸무게는 0보다 커야 합니다.")
    private Double weight;

    private UUID imageUploadId;

    private boolean hasMuzzle;
    private boolean hasLeash;
    private boolean hasCarrier;
}
