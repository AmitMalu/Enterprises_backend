package com.im.admin.request;

import lombok.Data;

@Data
public class AepsSendOtpRequest {
    private String retailerId;
    private String subProductId;
}
