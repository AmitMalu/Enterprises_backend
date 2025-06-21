package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PayoutTokenResponseDTO {

    private String status;
    private String message;
    private String code;
    private List<String> errors;
    private TokenData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenData {
        private String token;
        private String expire;
    }
}

