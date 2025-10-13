package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@AllArgsConstructor
@Data
public class DownlineBalanceDto {

    private String userId;
    private String name;
    private String parentAccount;
    private BigDecimal prepaidBalance;
    private BigDecimal postpaidBalance;
    private LocalDateTime createdOn;
}
