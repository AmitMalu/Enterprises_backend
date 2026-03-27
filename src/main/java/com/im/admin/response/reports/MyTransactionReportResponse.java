package com.im.admin.response.reports;

import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyTransactionReportResponse {

    private Integer id;
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private BigDecimal balBefore;
    private BigDecimal amount;
    private BigDecimal balAfter;

    private String subProductName;
    private String productName;

    private String schemeName;
    private String actionOnBalanceAmount;
    private Status status;
    private String tranxId;
    private String narration;
    private WalletType walletType;
    private String hierarchyId;

    private String vendorName;
    private String vendorTransactionId;
    private Double latitude;
    private Double longitude;



}
