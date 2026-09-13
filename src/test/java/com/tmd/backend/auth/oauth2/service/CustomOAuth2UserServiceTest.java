package com.tmd.backend.auth.oauth2.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomOAuth2UserServiceTest {

    @Test
    void usesProviderNicknameWhenPresent() {
        assertThat(CustomOAuth2UserService.resolveNickname("  댕댕이  ", "user@example.com"))
            .isEqualTo("댕댕이");
    }

    @Test
    void fallsBackToEmailLocalPartAndLimitsLength() {
        assertThat(CustomOAuth2UserService.resolveNickname(null, "abcdefghijklmnopqrstuvw@example.com"))
            .isEqualTo("abcdefghijklmnopqrst");
    }
}
