package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DashboardResponseDto {

    private BigDecimal totalTransaction;
    private BigDecimal totalFundTopUp;
    private BigDecimal pendingTransaction;
    private BigDecimal totalBalance;
    private Long totalNetwork;
    private Long pendingOnboarding;
}
