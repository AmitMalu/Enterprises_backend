package com.scs.admin.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class DebitCreditReportRequest {

    private String hierarchyId;
    private String hierarchyType;
    private String walletType;

    private LocalDate fromDate;
    private LocalDate toDate;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private LocalDate dateOfTransaction;  // exact date

    private LocalTime timeOfTransaction;
    private LocalTime fromTime;
    private LocalTime toTime;

    private String service;
    private String transactionId;
    private String transactionType;
    private BigDecimal amount;
    private BigDecimal balBeforeTran;
    private BigDecimal balAfterTran;
    private BigDecimal finalBalance;
}


