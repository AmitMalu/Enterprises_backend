package com.im.admin.request;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AepsBiometricEkycRequest {

//    private String retailerId;
private  Boolean isFacialTan;
    private String requestRemarks;
    private CaptureResponse captureResponse;
    private Integer superMerchantId;
    private String merchantLoginId;
    private String subProductId;


    private P2CardnumberORUID cardnumberORUID;

    private int primaryKeyId;
    private String encodeFPTxnId;

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
