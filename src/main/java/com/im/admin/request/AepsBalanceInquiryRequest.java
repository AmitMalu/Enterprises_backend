package com.im.admin.request;

import lombok.Data;

@Data
public class AepsBalanceInquiryRequest {

    private String subProductId;
    private String retailerId;             // From frontend
    private String requestRemarks;         // Optional
    private String transactionType;        // "BE"
    private String serviceType;            // "AEPS"
    private String latitude;
    private String longitude;
    private String paymentType;

    private String merchantUserName;       // Retailer username
    private String merchantPin;            // MD5 hashed merchant PIN
    private String merchantTranId;         // For cash withdrawal (not used here)
    private String merchantTransactionId;  // For balance enquiry (unique)
    private String mobileNumber;           // Customer mobile

    private long timestamp;                // System.currentTimeMillis()
    private int superMerchantId;
    private String merchantLoginId;

    private CaptureResponse captureResponse;
    private P2CardnumberORUID cardnumberORUID;

    // extra optional fields
    private String deviceIMEI;
    private String languageCode;

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

    @Data
    public static class P2CardnumberORUID {
        private String adhaarNumber;                   // Aadhaar number (12 digits)
        private int indicatorforUID;                  // 0 for Aadhaar , 2 for VID
        private String nationalBankIdentificationNumber;  // Bank IIN (Mandatory)
        private String virtualId;
    }
}


