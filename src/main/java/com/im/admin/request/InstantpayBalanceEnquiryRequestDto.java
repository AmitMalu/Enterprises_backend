package com.im.admin.request;

import lombok.Data;
import org.bouncycastle.asn1.x509.qualified.BiometricData;

import java.math.BigDecimal;

@Data
public class InstantpayBalanceEnquiryRequestDto {

    private String retailerId;
    private String subProductId;
    private String aadhaarNumber;
    private String bankiin;
    private String mobile;

    private BiometricData biometricData;
    private String schemeCode;
    private String transactionType;
    private BigDecimal transactionAmount;
    @Data
    public class BiometricData {

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
