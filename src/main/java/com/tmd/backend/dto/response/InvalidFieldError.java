package com.tmd.backend.dto.response;

import lombok.Getter;
import org.springframework.validation.BindingResult;

import java.util.List;

@Getter
public class InvalidFieldError {
    private String field;
    private String value;
    private String reason;

    private InvalidFieldError(String field, String value, String reason){
        this.field=field;
        this.value=value;
        this.reason=reason;
    }

    public static List<InvalidFieldError> of(BindingResult bindingResult){

        return bindingResult.getFieldErrors()
            .stream()
            .map(e -> new InvalidFieldError(
                e.getField(),
                e.getRejectedValue() == null ? "" : e.getRejectedValue().toString(),
                e.getDefaultMessage())
            )
            .toList();
    }

}
