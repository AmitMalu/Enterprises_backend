package com.scs.admin.response;

import lombok.Data;

@Data
public class Aeps2FaResponseDto {

    private boolean status;
    private String message;
    private Aeps2FaData data;  // updated
    private long statusCode;

    @Data
    public static class Aeps2FaData {
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
