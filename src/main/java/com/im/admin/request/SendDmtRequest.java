package com.im.admin.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SendDmtRequest {
    private Long retailerId;
    private String subProductId;
    private String walletType;
    private BigDecimal amount;
    private String beneficiaryName;
    private String accountNumber;
    private String mobile;
    private String txntype;
    private String pincode;
    private String address;
    private String dob;
    private String gstState;
    private String lat;
    private String lng;
    private String beneId;
    private String otp;
    private String referenceId;
    private String schemeCode;
    private String paymentMode;
}
