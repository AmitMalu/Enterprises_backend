package com.im.admin.exception;

import com.im.admin.dto.FieldErrorDetailsDTO;

import java.util.List;

public class CustomValidationException extends RuntimeException {
    private final List<FieldErrorDetailsDTO> errors;

    public CustomValidationException(List<FieldErrorDetailsDTO> errors) {
        this.errors = errors;
    }

    public List<FieldErrorDetailsDTO> getErrors() {
        return errors;
    }
}

