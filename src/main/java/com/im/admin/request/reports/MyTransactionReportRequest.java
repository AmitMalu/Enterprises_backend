package com.im.admin.request.reports;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.im.admin.dto.StatusDeserializer;
import com.im.admin.dto.WalletTypeDeserializer;
import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyTransactionReportRequest {

    private String hierarchyType;
    private String hierarchyId;   //ABC-RT-001 or RUT-DT-908 any hirarhcy id will come
    private String fromDate;   // Format: yyyy-MM-dd
    private String toDate;     // Format: yyyy-MM-dd
    private String subProductName;   //  PG, BBPS or ALL anything will come single subproduct or multiple or ALLs
    @JsonDeserialize(using = WalletTypeDeserializer.class)
    private WalletType walletType;   //PREPAID or POSTPAID or BOTH could be there
    @JsonDeserialize(using = StatusDeserializer.class)
    private Status status;  //PENDING or FAILURE or SUCCESS or ALL

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
