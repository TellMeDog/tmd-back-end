package com.tmd.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@JsonInclude(value = JsonInclude.Include.NON_NULL)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ResponseDto {
    private final boolean success;
    private final String message;
}
