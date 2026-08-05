package com.tmd.backend.auth.oauth2.info;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.exception.BaseException;

import java.util.Map;

public class OAuth2UserInfoFactory {
    public static OAuth2UserInfo getOAuth2UserInfo(String registrationId, Map<String, Object> attributes){
        switch (registrationId.toLowerCase()){
            case "google":
                return new GoogleOAuth2UserInfo(attributes);
            case "kakao":
                return new KakaoOAuth2UserInfo(attributes);
            case "naver":
                return new NaverOAuth2UserInfo(attributes);
            default:
                throw new BaseException(ErrorCode.INVALID_PROVIDER);
        }
    }
}
