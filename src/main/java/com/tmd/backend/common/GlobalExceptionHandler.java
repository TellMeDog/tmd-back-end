package com.tmd.backend.common;

import com.tmd.backend.dto.response.ErrorResponseDto;
import com.tmd.backend.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.server.authorization.HttpStatusServerAccessDeniedHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponseDto> handleBaseException(BaseException e){
        log.warn("BaseException: {}", e.getErrorCode());
        return ResponseEntity
            .status(e.getErrorCode().getHttpStatus())
            .body(ErrorResponseDto.error(e.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidationException(MethodArgumentNotValidException e){
        log.warn("ValidationException: {}", e);
        return ResponseEntity
            .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
            .body(ErrorResponseDto.validateBindingResult(e.getBindingResult()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleException(Exception e){
        log.error("Unexpected Error : {}", e);
        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus())
            .body(ErrorResponseDto.error(ErrorCode.INTERNAL_SERVER_ERROR));
    }
}
