package com.im.admin.response;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WalletBalanceResponseDto {

    private BigDecimal prepaidBalance;
    private BigDecimal postpaidBalance;

}
