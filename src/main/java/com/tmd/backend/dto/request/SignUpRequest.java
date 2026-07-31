package com.tmd.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SignUpRequest {

    @Email(message = "이메일 형식이 아닙니다.")
    @NotEmpty(message = "이메일을 입력해주세요.")
    private String email;

    @NotEmpty(message = "비밀번호를 입력해주세요.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,16}$",
        message = "비밀번호는 8~16자 영문 대소문자, 숫자, 특수문자를 최소 1개씩 포함해야 합니다.")
    private String password;

    @NotEmpty(message = "비밀번호 확인을 입력해주세요.")
    private String confirmPassword;

    @Builder
    public SignUpRequest(String email, String password, String confirmPassword){
        this.email=email;
        this.password=password;
        this.confirmPassword=confirmPassword;
    }
}
