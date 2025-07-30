package com.im.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendPayoutRequest {

    private String retailerId;
    private String accountNumber;
    private String ifscCode;
    private BigDecimal amount;
    private String orderId;
    private String beneficiaryName;
    private String paymentMode;
    private String subProductId;

}
