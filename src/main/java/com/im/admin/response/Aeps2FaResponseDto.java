package com.im.admin.response;

import lombok.Data;

@Data
public class Aeps2FaResponseDto {

    private boolean status;
    private String message;
    private Object data;
    private long statusCode;

    @lombok.Data
    public static class Data {
        private String tefPkId;
        private String stan;
        private String bankRrn;
        private String fpRrn;
        private String fingpayTransactionId;
        private String merchantTranId;
        private String responseCode;
        private String responseMessage;
        private String mobileNumber;
        private String transactionTimestamp;
    }
}
