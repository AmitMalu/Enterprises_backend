package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PgTokenResponseDTO {

    private String status;
    private String message;
    private String code;
    private TokenData data;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class TokenData {

        private String token;
        private int expiry;

    }
}
