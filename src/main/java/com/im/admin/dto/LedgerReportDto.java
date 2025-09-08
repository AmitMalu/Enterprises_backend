package com.im.admin.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerReportDto {

    private String id;
    private String transactionId;
    private String hierarchyId;
    private LocalDate dateOfTransaction;
    private LocalTime timeOfTransaction;
    private BigDecimal balBeforeTran;
    private BigDecimal amount;
    private BigDecimal balAfterTran;
    private BigDecimal finalBalance;
    private String tranStatus;
    private String actionOnBalanceAmount;
    private String service;
    private String mobileNo;
    private String subProductName;
    private String remarks;
    private String narration;
    private String sourceTable;

}

