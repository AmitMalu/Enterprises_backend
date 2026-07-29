package com.im.admin.request;

import lombok.*;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PayoutResponseDto {

    private String orderId;
    private String vendorName;
    private String status;
    private String message;
    private BigDecimal amount;
    private String beneficiaryName;
    private String accountNumber;
    private String bankName;
    private String ifscCode;

}

