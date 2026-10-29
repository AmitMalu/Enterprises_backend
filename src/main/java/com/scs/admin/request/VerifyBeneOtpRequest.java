package com.scs.admin.request;

import lombok.Data;

@Data
public class VerifyBeneOtpRequest {
    private String remitterMobile;          // remitterMobileNumber
    private String beneId;          // beneficiaryId
    private String referenceKey;    // from register response
    private String otp;
    private String outletId;        // optional (or fetch from DB)
    private String retailerId;
    private String subProductId;
    private String benename;     // Beneficiary name
    private String bankid;       // Bank ID (from bank list API)
    private String accno;        // Account number
    private String ifsccode;     // IFSC code
    private String verified;     // "0" or "1"
    private String gst_state;    // e.g., "07"
    private String dob;          // e.g., "1990-03-02"
    private String address;      // Beneficiary address
    private String pincode;      // Beneficiary pincode
    private String bankName;

}
