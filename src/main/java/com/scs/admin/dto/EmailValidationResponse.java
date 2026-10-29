package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EmailValidationResponse {
    private EmailData data;
    private int status_code;
    private boolean success;
    private String message;
    private String message_code;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class EmailData {
        private String client_id;
        private String email;
        private String status;
        private boolean valid;
        private boolean valid_syntax;
        private boolean accepts_mail;
        private boolean results;
        private boolean smtp_connected;
        private String domain;
        private String domain_age;
        private String domain_registrar;
        private String username;
        private boolean disabled;
        private String organization;
        private String organization_mactch;
        private String person_match;
        private boolean is_temporary;
        private boolean is_catch_all;
        private List<String> mx_records;
    }
}
