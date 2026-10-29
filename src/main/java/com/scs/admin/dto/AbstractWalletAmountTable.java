package com.scs.admin.dto;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@MappedSuperclass
public abstract class AbstractWalletAmountTable {
    protected BigDecimal totalCash;
    protected BigDecimal availableBalance;
    protected BigDecimal usedCash;
    protected BigDecimal lastUpdatedAmount;
    protected LocalDate lastUpdatedDate;
    protected LocalTime lastUpdatedTime;

}

