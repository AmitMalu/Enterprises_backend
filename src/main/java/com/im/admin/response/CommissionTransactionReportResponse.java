package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommissionTransactionReportResponse {

    private Integer id;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    //private String outletName;
    private String subProductName;
    private String walletType;
    private String txType;
    private BigDecimal balBefore;
    private BigDecimal amount;
    private BigDecimal balAfter;
    private String operator;
    private String txId;
    //private String narration;





}
