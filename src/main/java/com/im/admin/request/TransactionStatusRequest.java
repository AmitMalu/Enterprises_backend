package com.im.admin.request;

import lombok.Data;

@Data
public class TransactionStatusRequest {
    private String trackingType;   // TRANS_REF_ID
    private String trackingValue;  // CC015353BAAG00038670
    private String requestId;
}
