package com.im.admin.response;

import lombok.Data;

@Data
public class Instantpay2FaResponseDto {

    private String statuscode;
    private String actcode;
    private String status;

    private DataBlock data;

    private String timestamp;
    private String ipay_uuid;
    private String orderid;
//    private String environment;
//    private String internalCode;


    @Data
    public static class DataBlock {

        private Boolean isFaceAuthAvailable;
        private Boolean isIrisAuthAvailable;
        private String aadhaarLastFour;
        private Boolean isTxnBioLoginRequired;
        private String pidOptionFaceWadh;

        private String poolReferenceId;
        private String txnReferenceId;
//        private Pool pool;
    }

//    @Data
//    public static class Pool {
//
//        private String openingBalance;
//        private String closingBalance;
//        private String transactionValue;
//        private String payableValue;
//    }

}
