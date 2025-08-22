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
    private LocalDate updatedDateOfTransaction;
    private String retailerId;
    private String mobileNo;
    private String schemeName;
    private BigDecimal amount;
    private String tranStatus;
    private String transactionType;
    private String remarks;
    private String transactionId;
    private String medium;
    private String subProductName;

}