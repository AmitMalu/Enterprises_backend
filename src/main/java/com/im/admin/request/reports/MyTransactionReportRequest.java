package com.im.admin.request.reports;

import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MyTransactionReportRequest {

    private String hierarchyId;   //ABC-RT-001 or RUT-DT-908 any hirarhcy id will come
    private String fromDate;   // Format: yyyy-MM-dd
    private String toDate;     // Format: yyyy-MM-dd
    private String subProductName;   //  PG, BBPS or ALL anything will come single subproduct or multiple or ALLs
    private WalletType walletType;   //PREPAID or POSTPAID or BOTH could be there
    private Status status;  //PENDING or FAILURE or SUCCESS or ALL

    private int page = 0;
    private int size = 10;
}
