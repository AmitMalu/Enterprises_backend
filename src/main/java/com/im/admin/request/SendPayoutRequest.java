package com.im.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendPayoutRequest {

    private String accountNumber;
    private String ifscCode;
    private double amount;
    private String orderId;
    private String beneficiaryName;
    private String paymentMode;

}
