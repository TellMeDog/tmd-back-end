package com.tmd.backend.service;

import com.tmd.backend.auth.common.JwtToken;
import com.tmd.backend.auth.provider.JwtTokenProvider;
import com.tmd.backend.cache.SignUpCache;
import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.user.AuthProvider;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.request.LoginRequest;
import com.tmd.backend.dto.request.SignUpRequest;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RedisTemplate<String,Object> redisTemplate;
    private final MailService mailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    // 이메일 인증 번호 받기 버튼 클릭
    // 이메일 받은 후 -> 코드 생성 -> Cache 생성 -> Redis에 Key:Value 저장 -> 이메일 발송
    public void generateCodeAndSendMail(String email){
        // 코드 생성
        String code = String.valueOf((int) (Math.random() * 900000) + 100000);
        SignUpCache cache = new SignUpCache(code, false);
        redisTemplate.opsForValue().set(
            "signup:" + email,
            cache,
            Duration.ofMinutes(5)
            );
        mailService.sendVerificationEmail(email, code);
    }

    // 이메일 인증 완료 버튼 클릭
    // 이메일과 코드를 받은 후 -> Redis Cache의 code와 받은 code가 일치하면 isVerified=true 설정 후 TTL 연장 -> 그게 아니라면 Exception
    public void verifyEmail(String email, String code){
        SignUpCache cache = (SignUpCache) redisTemplate.opsForValue().get("signup:" + email);
        if(cache == null) throw new BaseException(ErrorCode.EXPIRED_TOKEN);
        if(cache.code().equals(code)) {
            SignUpCache newCache = new SignUpCache(code, true);
            redisTemplate.opsForValue().set(
                "signup:" + email,
                newCache,
                Duration.ofMinutes(10)
            );
        }else throw new BaseException(ErrorCode.INVALID_TOKEN);
    }

    @Transactional
    public void localSignUp(SignUpRequest signUpRequest){
        String email = signUpRequest.getEmail();
        String password = signUpRequest.getPassword();
        String confirmPassword = signUpRequest.getConfirmPassword();
        // 1. 이메일 중복 체크 -> 중복이면 BaseException(ErrorCdoe.DUPLICATE_EMAIL)
        if(userRepository.existsByEmailAndProvider(email, AuthProvider.LOCAL)){
            throw new BaseException(ErrorCode.DUPLICATE_EMAIL);
        }
        SignUpCache cache = (SignUpCache) redisTemplate.opsForValue().get("signup:" + email);
        if(cache == null || !cache.isVerified()) throw new BaseException(ErrorCode.NOT_VERIFIED_EMAIL);
        // 2. '비밀번호'와 '비밀번호 확인' 체크
        if(!password.equals(confirmPassword)){
            throw new BaseException(ErrorCode.PASSWORD_CONFIRM_FAIL);
        }
        // 3. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signUpRequest.getPassword());
        User user = User.createLocal(email, encodedPassword);
        userRepository.save(user);

        redisTemplate.delete("signup:" + email);
    }

    public JwtToken localLogin(LoginRequest loginRequest){
        String email = loginRequest.getEmail();
        User user = userRepository.findByEmailAndProvider(email, AuthProvider.LOCAL)
            .orElseThrow(() -> new BaseException(ErrorCode.LOGIN_FAIL));
        if(!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())){
            throw new BaseException(ErrorCode.LOGIN_FAIL);
        }
        log.info("로그인 성공: {}", email);
        // Controller에서 return된 JwtToken에서
        // access는 SuccessDto에 담아서 Return
        // refresh는 setCookie로 httpOnly에 담아줌.
        return issueToken(email);
    }

    public JwtToken reissue(String refreshToken){
        if(refreshToken == null){
            throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String email;

        try{
            email=jwtTokenProvider.getEmail(refreshToken);
        }catch(ExpiredJwtException e){
            throw new BaseException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }catch (JwtException e){
            throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String savedRefreshToken = (String) redisTemplate.opsForValue().get("refresh:" + email);
        if(savedRefreshToken == null || !savedRefreshToken.equals(refreshToken)){
            throw new BaseException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        String newAccessToken = jwtTokenProvider.createAccessToken(email);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(email);
        redisTemplate.opsForValue().set(
            "refresh:" + email,
            newRefreshToken,
            Duration.ofMillis(jwtTokenProvider.getRefreshExpiration())
        );

        return JwtToken.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .build();
    }

    public JwtToken issueToken(String email){
        String accessToken = jwtTokenProvider.createAccessToken(email);
        String refreshToken = jwtTokenProvider.createRefreshToken(email);
        redisTemplate.opsForValue().set(
            "refresh:" + email,
            refreshToken,
            Duration.ofMillis(jwtTokenProvider.getRefreshExpiration())
        );
        log.info("토큰 발급 : {}", email);

        return JwtToken.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .build();
    }
}
