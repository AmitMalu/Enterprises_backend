package com.im.admin.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PayoutTokenResponseDTO {

    private String status;
    private String message;
    private String code;
    private Object errors;
    private Object data;

    public TokenData getTokenData() {
        if (data instanceof java.util.Map) {
            java.util.Map<String, Object> dataMap = (java.util.Map<String, Object>) data;
            TokenData tokenData = new TokenData();
            tokenData.setToken((String) dataMap.get("token"));
            tokenData.setExpire((String) dataMap.get("expire"));
            return tokenData;
        }
        return null;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenData {
        private String token;
        private String expire;
    }
}

