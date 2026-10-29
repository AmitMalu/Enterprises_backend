package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AadhaarPanLinkResponse {
    private AadhaarPanLinkData data;
    private int status_code;
    private boolean success;
    private String message;
    private String message_code;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class AadhaarPanLinkData {
        private String client_id;
        private String masked_pan;
        private boolean linking_status;
        private String reason;
        private String detailed_reason;
    }
}

