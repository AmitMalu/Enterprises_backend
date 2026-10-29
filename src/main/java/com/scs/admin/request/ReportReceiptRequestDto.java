package com.scs.admin.request;

import lombok.Data;

@Data
public class ReportReceiptRequestDto {
    private String txnId;
    private String walletType;   // PREPAID / POSTPAID
    private Long id;

}
