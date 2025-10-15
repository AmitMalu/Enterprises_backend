package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class DebitCreditReportResponse {
    private LocalDate dateOfTransaction;
    private LocalTime timeOfTransaction;
    private BigDecimal balBeforeTran;
    private BigDecimal amount;
    private BigDecimal balAfterTran;
    private BigDecimal finalBalance;
    private String service;
    private String transactionId;
    private String transactionType;

}
