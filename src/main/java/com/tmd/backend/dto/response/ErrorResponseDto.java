package com.tmd.backend.dto.response;

import com.tmd.backend.common.ErrorCode;
import lombok.Getter;

@Getter
public class ErrorResponseDto extends ResponseDto{
    private final String code;

    private ErrorResponseDto(String code, String message){
        super(false, message);
        this.code=code;
    }

    public static ErrorResponseDto error(ErrorCode errorCode){
        return new ErrorResponseDto(errorCode.name(), errorCode.getMessage());
    }
}
