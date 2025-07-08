package com.im.admin.request.reports;


import com.im.admin.entities.enums.Status;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NetworkTransactionReportRequest {

    private String fromDate;   // Format: yyyy-MM-dd
    private String toDate;     // Format: yyyy-MM-dd
    private String productName;
    private String subProductName;
    private String enterprisesId;
    private String whitelabelId;
    private String superDistributorId;
    private String partnerId;
    private String distributorId;
    private String retailerId;
    private String loginHierarchyId;
    private Status status;

    private int page = 0;
    private int size = 10;
}
