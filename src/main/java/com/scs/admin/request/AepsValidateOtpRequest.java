package com.scs.admin.request;

import lombok.Data;

@Data
public class AepsValidateOtpRequest {
    private String retailerId;
    private String otp;
    private Integer primaryKeyId;
    private String encodeFPTxnId;
    private String subProductId;
}
