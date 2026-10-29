package com.scs.admin.entities.enums;

public enum InstantpayResponseMapper {

    SUCCESS,
    FAILED,
    PENDING;

    public static String map(String statusCode, String orderId) {

        // SUCCESS
        if ("TXN".equalsIgnoreCase(statusCode)
                && orderId != null
                && !orderId.isBlank()) {
            return SUCCESS.name();
        }

        // FAILED
        if (!"TXN".equalsIgnoreCase(statusCode)) {
            return FAILED.name();
        }

        // UNKNOWN
        return PENDING.name();
    }
}