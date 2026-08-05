package com.tmd.backend.auth.oauth2.handler;

import com.tmd.backend.auth.common.JwtToken;
import com.tmd.backend.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private AuthService authService;

    @Value("${oauth2.redirect-uri")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        String email = authentication.getName();
        JwtToken jwtToken = authService.issueToken(email);

        ResponseCookie refreshTokenCookie = ResponseCookie.from("refreshToken", jwtToken.getRefreshToken())
            .httpOnly(true)
            .secure(true)
            .sameSite("None")
            .path("/")
            .maxAge(Duration.ofDays(14))
            .build();

        response.setHeader("Set-Cookie", refreshTokenCookie.toString());

        String targetUrl = redirectUri + "?accessToken=" + jwtToken.getAccessToken();
        response.sendRedirect(targetUrl);

    }
}
