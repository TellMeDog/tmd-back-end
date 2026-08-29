package com.tmd.backend.dto.request.pet;

import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// enum 쓰려고 import 추가
import com.tmd.backend.domain.pet.PetBreed;
import com.tmd.backend.domain.pet.PetSize;
import jakarta.validation.constraints.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetRegisterRequest {

    @NotEmpty(message = "반려견 이름을 입력해주세요.")
    private String name;

    @NotNull(message = "품종을 선택해주세요.")  //Enum은 빈 문자열이 아니라 null인지로 검사
    private PetBreed breed;  // enum

    @NotNull(message = "크기를 선택해주세요.")  //Enum은 빈 문자열이 아니라 null인지로 검사
    private PetSize size;  // enum

    private String imageUrl;
}
