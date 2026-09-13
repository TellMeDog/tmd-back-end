package com.tmd.backend.auth.oauth2.info;

import com.tmd.backend.domain.user.AuthProvider;

import java.util.Map;

public class KakaoOAuth2UserInfo extends OAuth2UserInfo {

    public KakaoOAuth2UserInfo(Map<String, Object> attributes){
        super(attributes);
    }

    @Override
    public String getProviderId() {
        Object id = attributes.get("id");
        return id == null ? null : String.valueOf(id);
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.KAKAO;
    }

    @Override
    public String getEmail() {
        return stringValue(kakaoAccount().get("email"));
    }

    @Override
    public String getNickname() {
        Object profile = kakaoAccount().get("profile");
        if (profile instanceof Map<?, ?> profileMap) {
            String nickname = stringValue(profileMap.get("nickname"));
            if (nickname != null) {
                return nickname;
            }
        }
        Object properties = attributes.get("properties");
        if (properties instanceof Map<?, ?> propertiesMap) {
            return stringValue(propertiesMap.get("nickname"));
        }
        return null;
    }

    private Map<?, ?> kakaoAccount() {
        Object account = attributes.get("kakao_account");
        return account instanceof Map<?, ?> accountMap ? accountMap : Map.of();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
