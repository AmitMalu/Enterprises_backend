package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class AepsReceiptResponseDto
{
    private String serviceTitle;
    private String serviceKey;

    private String status;
    private String dateTime;
    private String transactionId;

    private String aadhaarNumber;
    private String mobileNumber;

    private String bankName;

    private Double transactionAmount;
    private Double balanceAmount;

    private String rrn;
    private String terminalId;

    private String errorMessage;
    private String statusMessage;
}

