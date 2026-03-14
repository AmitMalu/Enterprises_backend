package com.im.admin.response;

import lombok.Data;

@Data
public class InstantpayBiometricKycResponseDto {
    private String statuscode;
    private String actcode;
    private String status;
    private Object data;
    private String timestamp;
    private String ipay_uuid;
    private String orderid;
    private String environment;
}
