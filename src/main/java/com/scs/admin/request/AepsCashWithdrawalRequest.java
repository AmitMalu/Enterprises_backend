package com.scs.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AepsCashWithdrawalRequest {
    private  Boolean isFacialTan;
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
    private BigDecimal transactionAmount;
    private String schemeCode;
    private  String subMerchantId;
    private String adhaarNumber;
    private String nationalBankIdentificationNumber;


    private CaptureResponse captureResponse;
    private P2CardnumberORUID cardnumberORUID;

//    private String deviceIMEI;

    @Data
    public static class P2CardnumberORUID {
        private String adhaarNumber;
        private String indicatorforUID = "0";
        private String nationalBankIdentificationNumber;
    }

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

}
