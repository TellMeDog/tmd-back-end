package com.tmd.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VerificationCodeRequest {

    @Email(message = "이메일 형식이 아닙니다.")
    @NotEmpty(message = "이메일을 입력해주세요.")
    private String email;

    @NotEmpty(message = "인증번호를 입력해주세요.")
    private String code;

    @Builder
    public VerificationCodeRequest(String email, String code){
        this.email=email;
        this.code=code;
    }
}
