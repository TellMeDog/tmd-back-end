package com.tmd.backend.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    //400
    LOGIN_FAIL(HttpStatus.BAD_REQUEST, "이메일 혹은 비밀번호가 일치하지 않습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "잘못된 인증번호입니다."),
    EXPIRED_TOKEN(HttpStatus.BAD_REQUEST, "만료된 인증번호입니다."),
    PASSWORD_CONFIRM_FAIL(HttpStatus.BAD_REQUEST, "'비밀번호'와 '비밀번호 확인'이 일치하지 않습니다."),
    //401
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요한 서비스입니다."),
    //403
    NOT_VERIFIED_EMAIL(HttpStatus.FORBIDDEN, "이메일 인증이 필요합니다."),
    //409
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입한 이메일입니다.");



    private final HttpStatus httpStatus;
    private final String message;
}
