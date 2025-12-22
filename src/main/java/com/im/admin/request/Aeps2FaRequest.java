package com.im.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Aeps2FaRequest {

    private String subProductId;
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

    private String wadh;

    private long timestamp;                // System.currentTimeMillis()

    private String tefPkId;                // Reference ID (from validate API)
    private String fingpayTransactionId;   // From validate API

    private int superMerchantId;           // From backend
    private String merchantLoginId;        // Retailer ID from DB

    private CaptureResponse captureResponse;
    private P2CardnumberORUID cardnumberORUID;

    @Data
    public static class CaptureResponse {
        @JsonProperty("errCode")
        private String errCode;

        @JsonProperty("errInfo")
        private String errInfo;

        @JsonProperty("fCount")
        private String fCount;

        @JsonProperty("fType")
        private String fType;

        @JsonProperty("iCount")
        private String iCount;

        @JsonProperty("iType")
        private String iType;

        @JsonProperty("pCount")
        private String pCount;

        @JsonProperty("pType")
        private String pType;

        @JsonProperty("nmPoints")
        private String nmPoints;

        @JsonProperty("qScore")
        private String qScore;

        @JsonProperty("dpID")
        private String dpID;

        @JsonProperty("rdsID")
        private String rdsID;

        @JsonProperty("rdsVer")
        private String rdsVer;

        @JsonProperty("dc")
        private String dc;

        @JsonProperty("mi")
        private String mi;

        @JsonProperty("mc")
        private String mc;


        private String ci;

        @JsonProperty("sessionKey")
        private String sessionKey;

        @JsonProperty("hmac")
        private String hmac;

        @JsonProperty("PidDatatype")
        private String PidDatatype;

        @JsonProperty("Piddata")
        private String Piddata; // final piddata -> converted

    }

    @Data
    public static class P2CardnumberORUID {
        private String adhaarNumber;                   // Aadhaar number (999999999999 for testing)
        private int indicatorforUID;                  // 0 for Aadhaar , 2 for VID
        private String nationalBankIdentificationNumber;  // Bank IIN (Mandatory)
        private String virtualId;                     // ONLY IF indicatorforUID = 2
    }
}
