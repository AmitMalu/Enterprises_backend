package com.im.admin.request;

import lombok.Data;

import javax.sql.rowset.spi.SyncResolver;
import java.math.BigDecimal;

@Data
public class RechargeRequest {
    private String operatorCode;
    private String customerNumber;
    private BigDecimal amount;
    private String rechargeType; // S or 0
    private String subProductId;
    private String retailerId;
    private String transactionRef;
    private String schemeCode;
    private String operatorName;
}
