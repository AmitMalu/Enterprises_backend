package com.im.admin.dto;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.Time;
import java.util.Date;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractRetailerAmount {
    protected BigDecimal totalCash;
    protected BigDecimal availableBalance;
    protected BigDecimal usedCash;
    protected BigDecimal lastUpdatedAmount;
    protected Date lastUpdateDate;
    protected Time lastUpdateTime;
    protected BigDecimal taxValue;

}

