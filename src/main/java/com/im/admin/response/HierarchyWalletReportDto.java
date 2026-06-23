package com.im.admin.response;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * author abdul.M
 */
@Data
public class HierarchyWalletReportDto {

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

    private String     remark;

    private LocalDateTime createdOn;
    private String        createdBy;

    private LocalDate lastUpdatedDate;
}
