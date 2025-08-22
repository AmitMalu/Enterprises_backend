package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RetailerTransactionResponseDto {

    private Integer id;
    private LocalDate updatedAt;
    private String userId;
    private String mobileNo;
    private String schemeName;
    private BigDecimal amount;
    private String txStatus;
    private String txType;
    private String remarks;
    private String txId;
    private String medium;
    private String subProductName;

}