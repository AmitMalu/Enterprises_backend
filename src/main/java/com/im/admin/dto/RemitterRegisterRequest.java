package com.im.admin.dto;

import lombok.Data;

@Data
public class RemitterRegisterRequest {
    private String mobile;
    private String otp;
    private String stateresp;
    private String ekyc_id;
    private String subProductId;
    private String retailerId;
    private String aadhaarNumber;
    private String referenceKey;
}
