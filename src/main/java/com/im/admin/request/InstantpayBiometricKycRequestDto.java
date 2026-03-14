package com.im.admin.request;

import lombok.Data;

@Data
public class InstantpayBiometricKycRequestDto {
    private String retailerId;
    private String subProductId;
    private String latitude;
    private String longitude;
    private String aadhaarNumber;
    private String externalRef;
    private BiometricData biometricData;

    @Data
    public static class BiometricData {

        private String dc;
        private String ci;
        private String hmac;
        private String dpId;
        private String mc;
        private String pidDataType;
        private String rdsId;
        private String sessionKey;
        private String mi;
        private String errCode;
        private String errInfo;
        private String fCount;
        private String fType;
        private String iCount;
        private String iType;
        private String pCount;
        private String pType;
        private String srno;
        private String sysid;
        private String ts;
        private String pidData;
        private String qScore;
        private String nmPoints;
        private String rdsVer;

    }


    }
