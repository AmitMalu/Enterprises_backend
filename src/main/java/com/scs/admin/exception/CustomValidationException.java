package com.scs.admin.exception;

import com.scs.admin.dto.FieldErrorDetailsDTO;

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

