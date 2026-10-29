package com.scs.admin.dto;

import lombok.Data;

@Data
public class TransactionRequestDto {

    private String transactionId;
    private String hierarchyId;
    private String walletType;
}
