package com.scs.admin.response;

import lombok.Data;

@Data
public class AepsMerchantResponseDto {
    private boolean status;
    private String message;
    private DataDetails data;   // mapped object
    private Integer statusCode;

    @Data
    public static class DataDetails {
        private Boolean merchantStatus;
        private String remarks;
        private Integer superMerchantId;
        private String merchantLoginId;
        private String errorCodes;
    }
}
