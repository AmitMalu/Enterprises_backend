package com.im.admin.response;

import lombok.Data;

@Data
public class AepsMiniStatementResponse {

    private boolean status;
    private String message;
    private AepsBalanceInquiryResponse.BalanceData data;
    private long statusCode;

    @Data
    public static class BalanceData {
        private String terminalId;
        private String requestTransactionTime;
        private double transactionAmount;
        private String transactionStatus;
        private double balanceAmount;
        private String bankRRN;
        private String transactionType;
        private String fingpayTransactionId;
        private String merchantTxnId;
        private String responseCode;
        private String errorCode;
        private String errorMessage;
    }
}
