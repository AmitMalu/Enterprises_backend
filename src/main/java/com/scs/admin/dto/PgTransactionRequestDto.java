package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class PgTransactionRequestDto {

    private String cardBinNo;
    private String cardHolderMobileNumber;
    private BigDecimal amount;
    private String walletType; // optional: PREPAID/POSTPAID
    private Short productId;
    private String subProductId;
}

