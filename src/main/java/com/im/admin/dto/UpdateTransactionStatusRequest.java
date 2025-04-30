package com.im.admin.dto;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UpdateTransactionStatusRequest {

    private String tranxId;
    private String oldStatus;
    private String newStatus; // "SUCCESS" or "FAILED"
    private String walletType;  //
    private String failureRemark;

}

