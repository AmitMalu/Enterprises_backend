package com.im.admin.response;

import lombok.Data;

@Data
public class AepsEkycResponseDto {

    private boolean status;       // true/false
    private String message;       // success or error message
    private Integer statusCode;   // Fingpay code
    private EkycData data;        // nested object

    @Data
    public static class EkycData {
        private Boolean ekycStatus;      // true = success
        private String remarks;          // description
        private String encodeFPTxnId;    // fingpay txn id
        private Long primaryKeyId;       // unique fp id
    }
}
