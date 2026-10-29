package com.scs.admin.response;

import lombok.Data;

import java.util.List;

@Data
public class InstantpayBankListResponseDto {
    private String statuscode;
    private String actcode;
    private String status;
    private List<BankData> data;
    private String timestamp;
    private String ipay_uuid;
    private String orderid;
    private String environment;
    private String internalCode;

    @Data
    public static class BankData {

        private Long bankId;
        private String name;
        private String iin;
        private Boolean aepsEnabled;
        private Boolean aadhaarpayEnabled;
        private String aepsFailureRate;
        private String aadhaarpayFailureRate;
    }

}
