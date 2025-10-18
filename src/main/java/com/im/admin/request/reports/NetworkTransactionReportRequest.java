package com.im.admin.request.reports;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.im.admin.entities.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetworkTransactionReportRequest {

    private String fromDate;   // Format: yyyy-MM-dd
    private String toDate;     // Format: yyyy-MM-dd
    private String subProductName;
    private String enterprisesId;
    private String whitelabelId;
    private String superDistributorId;
    private String partnerId;
    private String distributorId;
    private String retailerId;
    private String loginHierarchyId;
    private Status status;

    private Integer id;
    private Short productId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd-MM-yyyy")
    private String transactionDate;
    private String transactionTime;
    private BigDecimal balBefore;
    private BigDecimal amount;
    private BigDecimal balAfter;
    private String actionOnBalanceAmount;
    private String tranxId;
    private String narration;
    private String walletTypeFilter;

    private int page = 0;
    private int size = 10;

}
