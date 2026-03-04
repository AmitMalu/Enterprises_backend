package com.im.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AepsBalanceInquiryRequest {

    private  Boolean isFacialTan;
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
//    private String merchantTranId;         // For cash withdrawal (not used here)
    private String merchantTransactionId;  // For balance enquiry (unique)
    private String mobileNumber;           // Customer mobile
    private String schemeCode;
    private BigDecimal transactionAmount;

    private String timestamp;                // System.currentTimeMillis()
    private int superMerchantId;
    private String merchantLoginId;

    private CaptureResponse captureResponse;
    private P2CardnumberORUID cardnumberORUID;
    private String adhaarNumber;

    // extra optional fields
    private String deviceIMEI;
    private String languageCode;

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
        private String adhaarNumber;                   // Aadhaar number (12 digits)
        private int indicatorforUID;                  // 0 for Aadhaar , 2 for VID
        private String nationalBankIdentificationNumber = "607153";
//        private String virtualId;
    }
}


