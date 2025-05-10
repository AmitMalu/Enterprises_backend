package com.im.admin.exception;

public class NoPendingRecordsFoundException extends RuntimeException {
    public NoPendingRecordsFoundException(String message) {
        super(message);
    }
}

