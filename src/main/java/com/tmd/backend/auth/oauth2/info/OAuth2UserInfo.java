package com.tmd.backend.auth.oauth2.info;

import com.tmd.backend.domain.user.AuthProvider;

import java.util.Map;

public abstract class OAuth2UserInfo {
    protected final Map<String, Object> attributes;

    protected OAuth2UserInfo(Map<String, Object> attributes){
        this.attributes=attributes;
    }

    public Map<String, Object> getAttributes(){
        return attributes;
    }

    public abstract String getProviderId();

    public abstract AuthProvider getProvider();

    public abstract String getEmail();
}
