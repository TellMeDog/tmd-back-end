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

        User user = userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() -> userRepository.save(
                User.createOAuth(email, provider, providerId)));

        return new CustomOAuth2User(user, oAuth2User.getAttributes());
    }
}
