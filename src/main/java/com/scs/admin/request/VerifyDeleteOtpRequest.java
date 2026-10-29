package com.scs.admin.request;

import lombok.Data;

@Data
public class VerifyDeleteOtpRequest {

    // Remitter Mobile Number
    private String mobile;

    // Beneficiary ID (InstantPay gives String, not Long)
    private String bene_id;

    // OTP received on mobile
    private String otp;

    // Reference Key received in delete API response
    private String referenceKey;

    // Outlet ID (required in headers)
    private String outletId;

    private String RetailerId;
}
