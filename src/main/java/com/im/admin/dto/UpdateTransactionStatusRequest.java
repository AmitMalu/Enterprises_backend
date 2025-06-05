package com.im.admin.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateTransactionStatusRequest {

    private String tranxId;
    private String oldStatus;
    private String newStatus; // "SUCCESS" to "FAILED" , FAILED to SUCCESS, PENDING to SUCCESS, SETTLEMENT PENDING to SUCCESS
    private String walletType;  //
    private String failureRemark;

}

