package com.im.admin.dto;

import lombok.Data;

@Data
public class ApiPanelRemitterRegisterRequest {

    private String mobile;
    private String otp;
    private String stateresp;
    private String ekyc_id;
    private String referenceKey;
    private String productId;
    private String apiPartnerId;
    private String aadhaarNumber;

}
