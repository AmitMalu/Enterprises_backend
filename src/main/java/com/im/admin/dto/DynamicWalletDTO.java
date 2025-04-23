package com.im.admin.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;

@Data
public class DynamicWalletDTO {
    private BigDecimal totalCash;
    private BigDecimal usedCash;
    private BigDecimal cutOfAmount;
    private BigDecimal availableBalance;
    private Date lastUpdateDate;
    private Time lastUpdateTime;
    private BigDecimal lastUpdatedAmount;
}

