package com.tmd.backend.controller;

import com.tmd.backend.auth.common.JwtToken;
import com.tmd.backend.dto.request.LoginRequest;
import com.tmd.backend.dto.request.SendVerificationCodeRequest;
import com.tmd.backend.dto.request.SignUpRequest;
import com.tmd.backend.dto.request.VerificationCodeRequest;
import com.tmd.backend.dto.response.LoginResponse;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.server.Cookie;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {
    private final AuthService authService;

    @PostMapping("/send-verification-code")
    public ResponseEntity<SuccessResponseDto<Void>> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest request){
        authService.generateCodeAndSendMail(request.getEmail().trim());
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("인증번호가 발송되었습니다."));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<SuccessResponseDto<Void>> verifyEmail(@Valid @RequestBody VerificationCodeRequest request){
        authService.verifyEmail(request.getEmail(), request.getCode());
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("이메일 인증이 완료되었습니다."));
    }

    @PostMapping("/signup")
    public ResponseEntity<SuccessResponseDto<Void>> signup(@Valid @RequestBody SignUpRequest signUpRequest){
        authService.localSignUp(signUpRequest);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("회원가입이 완료되었습니다."));
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponseDto<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest){
        JwtToken jwtToken = authService.localLogin(loginRequest);
        String accessToken = jwtToken.getAccessToken();
        String refreshToken = jwtToken.getRefreshToken();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(true) // true: only Https
            .sameSite("None") // None이 아닐 시, 다른 도메인/포트에서의 요청을 막음
            .path("/")
            .maxAge(Duration.ofDays(14))
            .build();

        LoginResponse response = LoginResponse.of(accessToken);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(SuccessResponseDto.success("로그인이 완료되었습니다.", response));
    }

    @PostMapping("/reissue")
    public ResponseEntity<SuccessResponseDto<LoginResponse>> reissue(
        @CookieValue(value = "refreshToken", required = false) String refreshToken){
        JwtToken newJwtToken = authService.reissue(refreshToken);

        String newAccessToken = newJwtToken.getAccessToken();
        String newRefreshToken = newJwtToken.getRefreshToken();

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", newRefreshToken)
            .httpOnly(true)
            .secure(true) // true: only Https
            .sameSite("None") // None이 아닐 시, 다른 도메인/포트에서의 요청을 막음
            .path("/")
            .maxAge(Duration.ofDays(14))
            .build();

        LoginResponse response = LoginResponse.of(newAccessToken);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(SuccessResponseDto.success("토큰이 재발급되었습니다.", response));
    }
}
