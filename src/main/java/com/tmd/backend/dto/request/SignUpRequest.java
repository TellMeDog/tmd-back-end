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

    // [수정] 필드 추가: 회원가입 시 사용자가 직접 입력하는 닉네임
    @NotEmpty(message = "닉네임을 입력해주세요.")
    @Size(min = 2, max = 20, message = "닉네임은 2~20자로 입력해주세요.")
    private String nickname;

    @Builder
    public SignUpRequest(String email, String password, String confirmPassword){
        // [수정] 매개변수에 nickname 추가
        this.email=email;
        this.password=password;
        this.confirmPassword=confirmPassword;
        this.nickname=nickname;  // [수정] nickname 대입 추가
    }
}
