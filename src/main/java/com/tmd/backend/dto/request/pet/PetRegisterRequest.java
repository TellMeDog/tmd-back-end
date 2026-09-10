package com.tmd.backend.dto.request.pet;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;  // size 검증용
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotNull;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PetRegisterRequest {

    @NotEmpty(message = "반려견 이름을 입력해주세요.")
    private String name;

    @NotEmpty(message = "품종을 입력해주세요.")
    private String breed;
    // [수정] PetBreed(Enum) → String
    // [수정] 검증 어노테이션: @NotNull → @NotEmpty
    //        (String 타입은 "빈 문자열인지"로 검사하는 @NotEmpty가 맞음.
    //         @NotNull은 null인지만 검사해서 빈 문자열("")은 통과시켜버림)

    @NotNull(message = "몸무게를 입력해주세요.")
    @Positive(message = "몸무게는 0보다 커야 합니다.")
    private Double size;
    // [수정] PetSize(Enum) → Double
    // [수정] 검증 어노테이션: 숫자 타입이라 @NotNull(null 아닌지) + @Positive(0보다 큰지) 조합 사용
    //        (Jakarta Bean Validation 공식 스펙: @Positive는 숫자 타입 전용 어노테이션)

    private String imageUrl;

    private boolean hasMuzzle;
    private boolean hasLeash;
    private boolean hasCarrier;
    // [수정] isVaccinated 필드 제거
}
