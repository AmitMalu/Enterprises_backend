package com.im.admin.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class  UpdateTransactionStatusRequest {


    //subProductName, schemeName, operatorName, traxId, status

    private List<String> transactionIds;
    private String oldStatus;
    private String newStatus; // "SUCCESS" to "FAILED" , FAILED to SUCCESS, PENDING to SUCCESS, SETTLEMENT PENDING to SUCCESS
    private String walletType;  //
    private String failureRemark;
    private String subProductName;
    private String operatorName;
    private String schemeName;


    public UpdateTransactionStatusRequest(List<String> transactionIds, String oldStatus, String newStatus, String walletType, String failureRemark) {
        this.transactionIds = transactionIds;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.walletType = walletType;
        this.failureRemark = failureRemark;
    }
}

