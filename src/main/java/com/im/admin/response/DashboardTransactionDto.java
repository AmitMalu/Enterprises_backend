package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardTransactionDto {
    private LocalDate transactionDate;
    private LocalTime transactionTime;
    private BigDecimal amount;
    private String productName;
    private String walletType;   // PREPAID / POSTPAID
    private String tranStatus;
}
