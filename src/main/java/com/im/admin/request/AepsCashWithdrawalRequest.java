package com.im.admin.request;

import lombok.Data;

@Data
public class AepsCashWithdrawalRequest {
    private String subProductId;
    private String retailerId;
    private String transactionType;
    private String serviceType;
    private String merchantUserName;
    private String merchantPin;
    private String merchantTranId;
    private String merchantLoginId;
    private String mobileNumber;
    private long timestamp;
    private int superMerchantId;
    private String paymentType;
    private String latitude;
    private String longitude;
    private String languageCode;
    private Double transactionAmount;

    private CaptureResponse captureResponse;
    private P2CardnumberORUID cardnumberORUID;

    private String deviceIMEI;

    @Data
    public static class P2CardnumberORUID {
        private String adhaarNumber;
        private int indicatorforUID;
        private String nationalBankIdentificationNumber;
    }

    @Data
    public static class CaptureResponse {
        private String errCode;
        private String errInfo;
        private String fCount;
        private String fType;
        private String iCount;
        private String iType;
        private String pCount;
        private String pType;
        private String nmPoints;
        private String qScore;
        private String dpID;
        private String rdsID;
        private String rdsVer;
        private String dc;
        private String mi;
        private String mc;
        private String ci;
        private String sessionKey;
        private String hmac;
        private String PidDatatype;
        private String Piddata;
    }

}
