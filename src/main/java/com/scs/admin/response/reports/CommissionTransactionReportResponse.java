package com.scs.admin.response.reports;

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
    private String productName;
    private String walletType;
    private String txType;
    private BigDecimal balBefore;
    private BigDecimal amount;
    private BigDecimal balAfter;
    private String operator;
    private String txId;
    private String narration;
    private String hierarchyId;

    private String vendorName;
    private String vendorTransactionId;
    private Double latitude;
    private Double longitude;





}
