package com.tmd.backend.dto.response;

import lombok.Getter;

@Getter
public class SuccessResponseDto<T> extends ResponseDto{
    private final T data;

    private SuccessResponseDto(String message, T data){
        super(true, message);
        this.data=data;
    }

    public static <T> SuccessResponseDto<T> success(String message, T data){
        return new SuccessResponseDto<T>(message, data);
    }

    public static SuccessResponseDto<Void> successWithoutData(String message){
        return new SuccessResponseDto<Void>(message, null);
    }
}
