package com.tmd.backend.auth.oauth2.info;

import com.tmd.backend.domain.user.AuthProvider;

import java.util.Map;

public class GoogleOAuth2UserInfo extends OAuth2UserInfo {

    public GoogleOAuth2UserInfo(Map<String, Object> attributes){
        super(attributes);
    }

    @Override
    public String getProviderId() {
        return (String) attributes.get("sub");
    }

    @Override
    public AuthProvider getProvider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }
}
