package com.scs.admin.request;

import lombok.Data;

@Data
public class AepsResendOtpRequest {
    private String retailerId;
    private Integer primaryKeyId;
    private String encodeFPTxnId;
    private String subProductId;
}
