package com.scs.admin.request;

import lombok.Data;

@Data
public class BeneficiaryRegisterRequest {
    private String mobile;       // Remitter mobile number
    private String benemobile;  // benificiary mobileno
    private String benename;     // Beneficiary name
    private String bankid;       // Bank ID (from bank list API)
    private String accno;        // Account number
    private String ifsccode;     // IFSC code
    private String verified;     // "0" or "1"
    private String gst_state;    // e.g., "07"
    private String dob;          // e.g., "1990-03-02"
    private String address;      // Beneficiary address
    private String pincode;      // Beneficiary pincode
    private String subProductId;
    private String bankName;
    private String retailerId;

    private String otp;              // used in verify API
    private String referenceKey;
}
