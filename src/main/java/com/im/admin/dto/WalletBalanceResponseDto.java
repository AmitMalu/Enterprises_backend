package com.im.admin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class WalletBalanceResponseDto {

    @JsonProperty("prepaid Balance")
    private BigDecimal prepaidBalance;

    @JsonProperty("postpaid Balance")
    private BigDecimal postpaidBalance;
}
