package com.tmd.backend.exception;

import com.tmd.backend.common.ErrorCode;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException{
    private final ErrorCode errorCode;

    protected BaseException(ErrorCode errorCode){
        this.errorCode=errorCode;
    }

    protected BaseException(ErrorCode errorCode, String message){
        super(message);
        this.errorCode = errorCode;
    }

    protected BaseException(ErrorCode errorCode, String message, Throwable cause){
        super(message, cause);
        this.errorCode=errorCode;
    }
}
