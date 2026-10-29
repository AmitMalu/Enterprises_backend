package com.scs.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WalletBalanceResponseDto {
    private BigDecimal prepaidBalance;
    private BigDecimal postpaidBalance;

}
