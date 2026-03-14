package com.im.admin.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class InstantpayMiniStatementRequestDto {

    private String retailerId;
    private String subProductId;

    private String bankiin;
    private String aadhaarNumber;
    private String mobile;
    private String schemeCode;
    private String transactionType;
    private BigDecimal transactionAmount;

    private InstantpayBiometricDataDto biometricData;

    @Data
    public class InstantpayBiometricDataDto {

        private String dc;
        private String ci;
        private String hmac;
        private String dpId;
        private String mc;
        private String pidDataType;
        private String sessionKey;
        private String mi;
        private String rdsId;
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
