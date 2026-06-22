package com.im.admin.request;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * author abdul.M
 */
@Data
public class HierarchyWalletReportRequest {
    private String hierarchyType;
    private String hierarchyId;

    private String fullName;
    private String outletCompanyName;

    private String mobileNumber;
    private String emailId;

    private BigDecimal prepaidBalance;
    private BigDecimal prepaidCutoffAmount;

    private BigDecimal postpaidBalance;
    private BigDecimal postpaidCutoffAmount;

    private LocalDateTime createdOn;
    private String createdBy;

    private LocalDateTime editedOn;
    private String editedBy;

    private int pageNo = 0;
    private int pageSize = 10;

}
