package com.scs.admin.response;

import lombok.Data;

@Data
public class InstantpayBalanceEnquiryResponseDto {

    private String statuscode;
    private String actcode;
    private String status;
    private DataField data;
    private String timestamp;
    private String ipay_uuid;
    private String orderid;
    private String environment;
    private String internalCode;


    @Data
    public static class DataField {

        private String externalRef;
        private String bankName;
        private String accountNumber;
        private String ipayId;
        private String transactionMode;
        private String payableValue;
        private String transactionValue;
        private String openingBalance;
        private String closingBalance;
        private String operatorId;
        private String walletIpayId;
        private String bankAccountBalance;

    }
}
