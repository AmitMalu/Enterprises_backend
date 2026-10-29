package com.scs.admin.exception;

public class NoPendingRecordsFoundException extends RuntimeException {
    public NoPendingRecordsFoundException(String message) {
        super(message);
    }
}

