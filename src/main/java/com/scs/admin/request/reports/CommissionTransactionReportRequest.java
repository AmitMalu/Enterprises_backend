package com.scs.admin.request.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.scs.admin.dto.WalletTypeDeserializer;
import com.scs.admin.entities.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommissionTransactionReportRequest {

    private String hierarchyType;
    private String hierarchyId;
    @JsonDeserialize(using = WalletTypeDeserializer.class)
    private WalletType walletType;
    private String fromDate;   // Format: yyyy-MM-dd
    private String toDate;     // Format: yyyy-MM-dd
    private String subProductName;
    private String reportType;   //COM, CHG
    private Short productId;

    private Integer id;
    private String txId;
    private String txType;
    private String operator;
    private Double amount;
    private Double balBefore;
    private Double balAfter;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private String transactionDate;   // Format: yyyy-MM-dd
    private String transactionTime;
    private String narration;

    private int page = 0;
    private int size = 10;
}
