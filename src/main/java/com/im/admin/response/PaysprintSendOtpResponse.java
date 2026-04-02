package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaysprintSendOtpResponse {
    private boolean status;
    private int response_code;
    private String message;
    private String stateresp;
    private String referenceId;
    private String referenceKey;
}
