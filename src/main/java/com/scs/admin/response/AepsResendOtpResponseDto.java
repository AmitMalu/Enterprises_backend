package com.scs.admin.response;

import lombok.Data;

@Data
public class AepsResendOtpResponseDto {
    private boolean status;
    private String message;
    private DataDetails data;   // mapped object
    private Long statusCode;

    @Data
    public static class DataDetails {
        private Integer primaryKeyId;
        private String encodeFPTxnId;
    }
}
