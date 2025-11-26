package com.im.admin.request;

import lombok.Data;

@Data
public class Aeps2FaRequest {

    private String retailerId;             // From frontend
    private String otp;                    // Only if OTP flow
    private int primaryKeyId;              // From Validate API
    private String encodeFPTxnId;          // From Validate API

    private String requestRemarks;         // Optional
    private String transactionType;        // AUO
    private String latitude;
    private String longitude;

    private String merchantUserName;       // Retailer username
    private String merchantPin;            // MD5 hashed merchant PIN
    private String merchantTranId;         // UNIQUE transaction reference
    private String mobileNumber;           // Customer mobile
    private String serviceType;            // AEPS or AP

    private long timestamp;                // System.currentTimeMillis()

    private String tefPkId;                // Reference ID (from validate API)
    private String fingpayTransactionId;   // From validate API

    private int superMerchantId;           // From backend
    private String merchantLoginId;        // Retailer ID from DB

    private CaptureResponse captureResponse;
    private P2CardnumberORUID cardnumberORUID;

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
        private String adhaarNumber;                   // Aadhaar number (999999999999 for testing)
        private int indicatorforUID;                  // 0 for Aadhaar , 2 for VID
        private String nationalBankIdentificationNumber;  // Bank IIN (Mandatory)
        private String virtualId;                     // ONLY IF indicatorforUID = 2
    }
}
