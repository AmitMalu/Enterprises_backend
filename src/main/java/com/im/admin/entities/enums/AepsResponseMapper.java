package com.im.admin.entities.enums;

public enum AepsResponseMapper {

    SUCCESS,
    FAILED,
    PENDING;

    public static String map(boolean status, String responseCode, String bankRRN) {

        // --- SUCCESS LOGIC ---
        if (status && "00".equals(responseCode) && bankRRN != null && !bankRRN.isBlank()) {
            return SUCCESS.name();
        }

        // --- FAILED LOGIC ---
        if (!status || !"00".equals(responseCode)) {
            return FAILED.name();
        }

        // --- UNKNOWN => PENDING ---
        return PENDING.name();
    }
}
