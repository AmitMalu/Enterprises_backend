package com.im.admin.request;

import lombok.Data;

@Data
public class DashboardTransactionRequest {
    private String hierarchyId;
    private String fromDate;
    private String toDate;

}
