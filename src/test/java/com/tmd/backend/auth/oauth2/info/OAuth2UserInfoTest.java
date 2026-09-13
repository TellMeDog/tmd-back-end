package com.tmd.backend.auth.oauth2.info;

import com.tmd.backend.domain.user.AuthProvider;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OAuth2UserInfoTest {

    @Test
    void googleProfile() {
        OAuth2UserInfo info = new GoogleOAuth2UserInfo(Map.of(
            "sub", "google-id",
            "email", "google@example.com",
            "name", "구글 사용자"
        ));

        assertThat(info.getProvider()).isEqualTo(AuthProvider.GOOGLE);
        assertThat(info.getProviderId()).isEqualTo("google-id");
        assertThat(info.getEmail()).isEqualTo("google@example.com");
        assertThat(info.getNickname()).isEqualTo("구글 사용자");
    }

    @Test
    void kakaoProfile() {
        OAuth2UserInfo info = new KakaoOAuth2UserInfo(Map.of(
            "id", 12345L,
            "kakao_account", Map.of(
                "email", "kakao@example.com",
                "profile", Map.of("nickname", "카카오 사용자")
            )
        ));

        assertThat(info.getProvider()).isEqualTo(AuthProvider.KAKAO);
        assertThat(info.getProviderId()).isEqualTo("12345");
        assertThat(info.getEmail()).isEqualTo("kakao@example.com");
        assertThat(info.getNickname()).isEqualTo("카카오 사용자");
    }

    @Test
    void naverProfile() {
        OAuth2UserInfo info = new NaverOAuth2UserInfo(Map.of(
            "response", Map.of(
                "id", "naver-id",
                "email", "naver@example.com",
                "nickname", "네이버 사용자"
            )
        ));

        assertThat(info.getProvider()).isEqualTo(AuthProvider.NAVER);
        assertThat(info.getProviderId()).isEqualTo("naver-id");
        assertThat(info.getEmail()).isEqualTo("naver@example.com");
        assertThat(info.getNickname()).isEqualTo("네이버 사용자");
    }
}
