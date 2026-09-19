package com.tmd.backend.auth.oauth2.service;

import com.tmd.backend.auth.oauth2.user.CustomOAuth2User;
import com.tmd.backend.auth.oauth2.info.OAuth2UserInfo;
import com.tmd.backend.auth.oauth2.info.OAuth2UserInfoFactory;
import com.tmd.backend.domain.user.AuthProvider;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.repository.user.UserRepository;
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
        String nickname = resolveNickname(oAuth2UserInfo.getNickname(), email);

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() -> userRepository.save(
                User.createOAuth(email, provider, providerId, nickname)));

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }

    static String resolveNickname(String providerNickname, String email) {
        String nickname = providerNickname == null ? "" : providerNickname.strip();
        if (nickname.isEmpty() && email != null) {
            int separator = email.indexOf('@');
            nickname = (separator > 0 ? email.substring(0, separator) : email).strip();
        }
        if (nickname.isEmpty()) {
            nickname = "user";
        }

        int codePointCount = nickname.codePointCount(0, nickname.length());
        if (codePointCount <= 20) {
            return nickname;
        }
        int endIndex = nickname.offsetByCodePoints(0, 20);
        return nickname.substring(0, endIndex);
    }
}
