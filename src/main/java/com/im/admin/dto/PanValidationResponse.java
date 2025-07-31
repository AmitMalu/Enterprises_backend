package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PanValidationResponse {
    private PanData data;
    private int status_code;
    private String message;
    private boolean success;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PanData {
        private String pan_number;
        private String full_name;
        private String client_id;
        private String category;
    }
}
