package com.tmd.backend.controller;

import com.tmd.backend.dto.request.SendVerificationCodeRequest;
import com.tmd.backend.dto.request.SignUpRequest;
import com.tmd.backend.dto.request.VerificationCodeRequest;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}
