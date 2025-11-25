package com.im.admin.request;

import lombok.Data;

@Data
public class AepsCombinedRequest {

    // Validate OTP fields
    private String retailerId;
    private String otp;
    private Integer primaryKeyId;
    private String encodeFPTxnId;

    // Biometric fields
    private String requestRemarks;
    private AepsBiometricEkycRequest.P2CardnumberORUID cardnumberORUID;
    private AepsBiometricEkycRequest.CaptureResponse captureResponse;
}
