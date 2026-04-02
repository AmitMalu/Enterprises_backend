package com.im.admin.request;

import lombok.Data;

@Data
public class VerifyBeneOtpRequest {
    private String mobile;          // remitterMobileNumber
    private String beneId;          // beneficiaryId
    private String referenceKey;    // from register response
    private String otp;
    private String outletId;        // optional (or fetch from DB)
    private String retailerId;
}
