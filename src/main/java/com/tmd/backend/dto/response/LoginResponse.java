package com.tmd.backend.dto.response;

import lombok.Getter;

@Getter
public class LoginResponse {
    private final String accessToken;

    private LoginResponse(String accessToken){
        this.accessToken=accessToken;
    }

    public static LoginResponse of(String accessToken){
        return new LoginResponse(accessToken);
    }
}
