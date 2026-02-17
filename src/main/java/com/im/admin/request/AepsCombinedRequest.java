package com.im.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class AepsCombinedRequest {

    private  Boolean isFacialTan;
    private String retailerId;
    private String aadhaar;
    private String otp;
    private Integer primaryKeyId;
    private String encodeFPTxnId;
    private String deviceType;

    private String subProductId;
    private String requestRemarks;

    private CaptureResponse captureResponse;

    @Data
    public static class CaptureResponse {

        private String errCode;
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

        @JsonProperty("ci")
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
