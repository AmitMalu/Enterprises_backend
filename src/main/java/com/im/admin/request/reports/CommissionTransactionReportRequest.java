package com.im.admin.request.reports;

import com.im.admin.entities.enums.WalletType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommissionTransactionReportRequest {

    private String hierarchyId;
    private WalletType walletType;
    private String fromDate;   // Format: yyyy-MM-dd
    private String toDate;     // Format: yyyy-MM-dd
    private String subProductName;
    private String reportType;   //COM, CHG

    private int page = 0;
    private int size = 10;
}
