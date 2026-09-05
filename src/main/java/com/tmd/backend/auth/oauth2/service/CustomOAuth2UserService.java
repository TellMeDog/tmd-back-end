package com.tmd.backend.auth.oauth2.service;

import com.tmd.backend.auth.oauth2.user.CustomOAuth2User;
import com.tmd.backend.auth.oauth2.info.OAuth2UserInfo;
import com.tmd.backend.auth.oauth2.info.OAuth2UserInfoFactory;
import com.tmd.backend.domain.user.AuthProvider;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId(); // Google, Kakao 같은 provider

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());

        String email = oAuth2UserInfo.getEmail();
        AuthProvider provider = oAuth2UserInfo.getProvider();
        String providerId = oAuth2UserInfo.getProviderId();

        // email 중복 방지 로직을 위해 코드 수정
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() -> {
                // 이 provider로는 처음이지만, 혹시 다른 provider로 같은 이메일이 이미 가입되어 있는지 먼저 확인
                userRepository.findByEmail(email)
                    .ifPresent(existingUser -> {  // 만약 이미 그 이메일로 가입된 User가 있으면 (provider가 뭐든 상관없이) 예외를 던져서 새 계정 생성을 막음
                        throw new OAuth2AuthenticationException(  // Spring Security의 OAuth2 인증 과정에서 쓰는 표준 예외 타입 (여기서는 이 타입으로 던져야 Spring Security 흐름에 맞음)
                            "이미 " + existingUser.getProvider() + "(으)로 가입된 이메일입니다."
                        );
                    });

                return userRepository.save(User.createOAuth(email, provider, providerId));
            });

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }
}
