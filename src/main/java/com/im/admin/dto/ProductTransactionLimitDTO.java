package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ProductTransactionLimitDTO {

    private Short productId;
    private Integer dailyTransactionLimit;
    private Integer monthlyTransactionLimit;
    private Integer yearlyTransactionLimit;
    private BigDecimal maxPerTransactionAmt;
}
