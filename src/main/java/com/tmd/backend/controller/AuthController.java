package com.tmd.backend.controller;

import com.tmd.backend.dto.request.SignUpRequest;
import com.tmd.backend.dto.response.SuccessResponseDto;
import com.tmd.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RequestMapping("/auth")
@RestController
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<SuccessResponseDto<Void>> signup(@Valid @RequestBody SignUpRequest signUpRequest){
        authService.localSignUp(signUpRequest);
        return ResponseEntity.ok(SuccessResponseDto.successWithoutData("회원가입이 완료되었습니다."));
    }
}
