package com.tmd.backend.dto.request.pet;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetRegisterRequest {
    @NotEmpty(message = "반려견 이름을 입력해주세요.")
    private String name;

    @NotEmpty(message = "견종을 선택해주세요.")
    private String breed;

    @NotEmpty(message = "크기를 선택해주세요.")
    private String size;

    private UUID imageUploadId;
}
