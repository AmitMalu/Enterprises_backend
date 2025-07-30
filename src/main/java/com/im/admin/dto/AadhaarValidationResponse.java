package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AadhaarValidationResponse {
    private AadhaarData data;
    private int status_code;
    private boolean success;
    private String message;
    private String message_code;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AadhaarData {
        private String client_id;
        private String age_range;
        private String aadhaar_number;
        private String state;
        private String gender;
        private String last_digits;
        private boolean is_mobile;
        private String remarks;
        private boolean less_info;
    }
}
