package com.im.admin.dto;

import lombok.Data;

@Data
public class RemitterRegisterResponse {

    private boolean status;
    private int response_code;
    private String message;
    private DataObj data;

    @Data
    public static class DataObj {
        private String remitter_id;
        private String name;
        private String mobile;
        private String kyc_status;
        private String otp_status;
        private Double limit;

    }
}
