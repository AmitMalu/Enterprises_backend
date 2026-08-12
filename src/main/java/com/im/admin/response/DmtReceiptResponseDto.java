package com.im.admin.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DmtReceiptResponseDto {
    private String status;
    private String dateTime;

    private String txnType;

    private String beneficiaryName;

    private Double amount;
    private String accNumber;

    private String orderId;
    private String vendorTxnId;
    private String utr;
    private String referenceId;

    private String message;
    private String remarks;
    private String ifsc;
}
