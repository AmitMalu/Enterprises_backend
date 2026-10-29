package com.scs.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class RemitterEkycRequest {

    @JsonProperty("mobile")
    private String mobile;          // Remitter mobile number

    @JsonProperty("lat")
    private String lat = "28.123456"; // Latitude

    // "long" is a Java keyword → map it safely
    @JsonProperty("long")
    private String longitude = "78.123456";

    @JsonProperty("aadhaar_number")
    private String aadhaarNumber;   // Aadhaar number

    @JsonProperty("data")
    private String data;            // Encrypted PID XML

    @JsonProperty("accessmode")
    private String accessmode = "WEB";

    @JsonProperty("is_iris")
    private Integer isIris = 2;      // 2 = Finger default

    // if vendor expects this EXACT key, keep annotation
    @JsonProperty("subProductId")
    private String subProductId;

    @JsonProperty("name")
    private String name;

    private String otp;
    private String referenceKey;
    private String retailerId;

    private BiometricData biometricData;

    @Data
    public static class BiometricData {

        @JsonProperty("ci")
        private String ci;

        @JsonProperty("hmac")
        private String hmac;

        @JsonProperty("Piddata")
        private String Piddata;

        @JsonProperty("ts")
        private String ts;

        @JsonProperty("dc")
        private String dc;

        @JsonProperty("mi")
        private String mi;

        @JsonProperty("dpID")
        private String dpID;

        @JsonProperty("mc")
        private String mc;

        @JsonProperty("rdsID")
        private String rdsID;

        @JsonProperty("rdsVer")
        private String rdsVer;

//        @JsonProperty("Skey")   // IMPORTANT (case-sensitive)
//        private String Skey;

        @JsonProperty("sessionKey")   // IMPORTANT (case-sensitive)
        private String sessionKey;

        @JsonProperty("srno")
        private String srno;

    }


}
