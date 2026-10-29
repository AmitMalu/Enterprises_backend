package com.scs.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Instantpay2FaRequestDto {

    private String retailerId;
    private String subProductId;
    private String aadhaarNumber;
    private BiometricData biometricData;

    @Data
    public class BiometricData {

        @JsonProperty("dc")
        private String dc;

        @JsonProperty("ci")
        private String ci;

        @JsonProperty("hmac")
        private String hmac;

//        @JsonProperty("dpId")
//        private String dpId;

        @JsonProperty("dpID")
        private String dpID;

        @JsonProperty("mc")
        private String mc;

//        @JsonProperty("pidDataType")
//        private String pidDataType;

        @JsonProperty("PidDatatype")
        private String PidDatatype;



        @JsonProperty("rdsID")
        private String rdsID;

        @JsonProperty("sessionKey")
        private String sessionKey;

        @JsonProperty("mi")
        private String mi;

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

        @JsonProperty("srno")
        private String srno;

        @JsonProperty("sysid")
        private String sysid;

        @JsonProperty("ts")
        private String ts;

        @JsonProperty("Piddata")
        private String Piddata;

        @JsonProperty("qScore")
        private String qScore;

        @JsonProperty("nmPoints")
        private String nmPoints;

        @JsonProperty("rdsVer")
        private String rdsVer;

    }
}
