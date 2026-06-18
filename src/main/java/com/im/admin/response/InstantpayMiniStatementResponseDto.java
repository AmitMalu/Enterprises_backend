package com.im.admin.response;

import lombok.Data;

import java.util.List;

@Data
public class InstantpayMiniStatementResponseDto {

    private String statuscode;
    private String actcode;
    private String status;

    private InstantpayMiniStatementDataDto data;

    private String timestamp;
    private String ipay_uuid;
    private String orderid;
    private String environment;
    private String internalCode;

    @Data
    public static class InstantpayMiniStatementDataDto {

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

        private List<InstantpayMiniTxnDto> miniStatement;
    }

    @Data
    public static class InstantpayMiniTxnDto {

        private String date;
        private String txnType;
        private String amount;
        private String narration;
    }
}