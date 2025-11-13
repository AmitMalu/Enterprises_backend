package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class PaysprintSendOtpResponse {
    private boolean status;
    private int response_code;
    private String message;
    private String stateresp;
}
