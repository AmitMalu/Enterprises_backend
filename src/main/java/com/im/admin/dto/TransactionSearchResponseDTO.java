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
public class TransactionSearchResponseDTO {

    private String transactionId;
    private String hierarchyId;
    private String tranStatus;
    private String walletType;
    private BigDecimal amount;
    private String operatorName;
}

