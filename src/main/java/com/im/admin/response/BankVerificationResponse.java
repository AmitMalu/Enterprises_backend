package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankVerificationResponse {
    private BankData data;
    private int status_code;
    private boolean success;
    private String message;
    private String message_code;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BankData {
        private String client_id;
        private boolean account_exists;
        private String upi_id;
        private String full_name;
        private String imps_ref_no;
        private String remarks;
        private String status;
        private IfscDetails ifsc_details;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class IfscDetails {
        private int id;
        private String ifsc;
        private String micr;
        private String iso3166;
        private String swift;
        private String bank;
        private String bank_code;
        private String bank_name;
        private String branch;
        private String centre;
        private String district;
        private String state;
        private String city;
        private String address;
        private String contact;
        private boolean imps;
        private boolean rtgs;
        private boolean neft;
        private boolean upi;
        private boolean micr_check;
    }
}
