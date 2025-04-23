package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DynamicTransactionDTO {
    private String transactionId;
    private BigDecimal amount;
    private BigDecimal balBeforeTran;
    private BigDecimal balAfterTran;
    private BigDecimal finalBalance;
    private String tranStatus;
    private String actionOnBalanceAmount;
    private String service;
    private String operatorName;
    private String subProductName;
    private String schemeName;
    private String mobileNo;
    private String remarks;
    private String narration;
    private String hierarchyId;
}

