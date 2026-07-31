package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.user.AuthProvider;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.request.SignUpRequest;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void localSignUp(SignUpRequest signUpRequest){
        String email = signUpRequest.getEmail();
        String password = signUpRequest.getPassword();
        String confirmPassword = signUpRequest.getConfirmPassword();
        // 1. 이메일 중복 체크 -> 중복이면 BaseException(ErrorCdoe.DUPLICATE_EMAIL)
        if(userRepository.existsByEmailAndProvider(email, AuthProvider.LOCAL)){
            throw new BaseException(ErrorCode.DUPLICATE_EMAIL);
        }
        // 2. '비밀번호'와 '비밀번호 확인' 체크
        if(!password.equals(confirmPassword)){
            throw new BaseException(ErrorCode.PASSWORD_CONFIRM_FAIL);
        }
        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signUpRequest.getPassword());
        // 3. User.createLocal
        User user = User.createLocal(email, encodedPassword);
        // 4. userRepository.save(user)
        userRepository.save(user);
        log.info("회원 가입 완료: email={}", email);
    }
}
