package com.tmd.backend.auth.oauth2.info;

import com.tmd.backend.domain.user.AuthProvider;

import java.util.Map;

public class NaverOAuth2UserInfo extends OAuth2UserInfo {

    public NaverOAuth2UserInfo(Map<String, Object> attributes){
        super(attributes);
    }

    @Override
    public String getProviderId() {
        return stringValue(response().get("id"));
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.NAVER;
    }

    @Override
    public String getEmail() {
        return stringValue(response().get("email"));
    }

    @Override
    public String getNickname() {
        Object nickname = response().get("nickname");
        return stringValue(nickname != null ? nickname : response().get("name"));
    }

    private Map<?, ?> response() {
        Object response = attributes.get("response");
        return response instanceof Map<?, ?> responseMap ? responseMap : attributes;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
