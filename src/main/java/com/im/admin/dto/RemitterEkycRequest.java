package com.im.admin.dto;

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

        private String ci;
        private String hmac;
        private String pidData;
        private String ts;
        private String dc;
        private String mi;
        private String dpId;
        private String mc;
        private String rdsId;
        private String rdsVer;
        private String Skey;
        private String srno;
    }


}
