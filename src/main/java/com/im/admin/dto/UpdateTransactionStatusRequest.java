package com.im.admin.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class  UpdateTransactionStatusRequest {

    private List<String> transactionIds;
    private String oldStatus;
    private String newStatus; // "SUCCESS" to "FAILED" , FAILED to SUCCESS, PENDING to SUCCESS, SETTLEMENT PENDING to SUCCESS
    private String walletType;  //
    private String failureRemark;

}

