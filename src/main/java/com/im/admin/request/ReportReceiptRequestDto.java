package com.im.admin.request;

import lombok.Data;

@Data
public class ReportReceiptRequestDto {
    private String txnId;
    private String walletType;   // PREPAID / POSTPAID
}
