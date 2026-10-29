package com.scs.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;

@Data
public class RefundTransactionDto {
    private Integer id;
    private BigDecimal amount;
    private BigDecimal balAfterTran;
    private BigDecimal balBeforeTran;
    private Date dateOfTransaction;
    private BigDecimal finalBalance;
    private Time timeOfTransaction;
    private Date updatedDateOfTransaction;
    private Time updatedTimeOfTransaction;
    private String actionOnBalanceAmount;
    private String bankRefId;
    private String operatorName;
    private String userId;
    private String schemeHierarchyName;
    private String schemeName;
    private String service;
    private String subOperatorName;
    private String subProductName;
    private String tranStatus;
    private String transactionId;
    private String transactionType;
}
