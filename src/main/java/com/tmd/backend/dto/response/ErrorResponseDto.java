package com.tmd.backend.dto.response;

import com.tmd.backend.common.ErrorCode;
import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.util.List;

@Getter
public class ErrorResponseDto extends ResponseDto{
    private final String code;
    private List<InvalidFieldError> invalidFieldErrors;

    private ErrorResponseDto(String code, String message){
        super(false, message);
        this.code=code;
    }

    private ErrorResponseDto(ErrorCode errorCode, List<InvalidFieldError> invalidFieldErrors){
        super(false, errorCode.getMessage());
        this.invalidFieldErrors=invalidFieldErrors;
        this.code=errorCode.name();
    }

    public static ErrorResponseDto error(ErrorCode errorCode){
        return new ErrorResponseDto(errorCode.name(), errorCode.getMessage());
    }

    public static ErrorResponseDto validateBindingResult(BindingResult bindingResult){
        return new ErrorResponseDto(ErrorCode.VALIDATION_ERROR, InvalidFieldError.of(bindingResult));
    }
}
