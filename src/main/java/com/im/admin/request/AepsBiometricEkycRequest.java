package com.im.admin.request;

import lombok.Data;

@Data
public class AepsBiometricEkycRequest {

    private String retailerId;
    private P2CardnumberORUID cardnumberORUID;
    private String requestRemarks;
    private CaptureResponse captureResponse;

    private String merchantLoginId;   // will be overridden from backend
    private int superMerchantId;      // will be overridden from backend
    private int primaryKeyId;
    private String encodeFPTxnId;

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
