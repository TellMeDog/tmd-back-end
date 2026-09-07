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

// 이메일 중복 방지 로직 위해 추가
import org.springframework.security.oauth2.core.OAuth2Error;

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
                userRepository.findByEmail(email)
                    .ifPresent(existingUser -> {
                        throw new OAuth2AuthenticationException(
                            new OAuth2Error(
                                "duplicate_email",
                                // 규격화된 짧은 에러 코드
                                "이미 " + existingUser.getProvider() + "(으)로 가입된 이메일입니다.",
                                // 실제 사람이 읽을 설명 메시지
                                null
                                // 에러 참고 URI (없으므로 null)
                            )
                        );
                    });

                return userRepository.save(User.createOAuth(email, provider, providerId));
            });

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }
}
