package com.scs.admin.response;

import lombok.Data;

@Data
public class RechargeCallbackData {
    private String ApiCode;
    private String transactionRef;          // transactionRef (TNO)
    private String status;           // ST
    private String statusMessage;    // STMSG
    private String vendorTxnId;      // TID
    private String operatorTxnId;    // OPRTID
    private String preBalance;       // PRB
    private String postBalance;      // POB
    private String discountPercent;  // DP
    private String discountRs;       // DR
}
