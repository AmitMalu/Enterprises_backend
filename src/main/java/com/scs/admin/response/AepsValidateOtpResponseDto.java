package com.scs.admin.response;

import lombok.Data;

@Data
public class AepsValidateOtpResponseDto {
    private boolean status;
    private String message;
    private Long statusCode;
    private DataDetails data;

    @Data
    public static class DataDetails {
        private Integer primaryKeyId;
        private String encodeFPTxnId;
    }
}
