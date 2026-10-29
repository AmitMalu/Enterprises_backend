package com.scs.admin.response;

import lombok.Data;

@Data
public class InstantpayKycStatusResponseDto {

    private String statuscode;
    private String actcode;
    private String status;
    private Data data;
    private String timestamp;
    private String ipay_uuid;
    private String orderid;
    private String environment;

    @lombok.Data
    public static class Data {
        private String outletId;
        private String outletAadhaarNumber;
        private String action;
        private String status;
        private Boolean isFaceAuthAvailable;
        private Boolean isBiometricKycMandatory;
        private String pidOptionWadh;
        private String referenceKey;
        private String referenceKeyType;
    }

}
