package com.scs.admin.response;

import lombok.Data;

import java.util.List;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
public class BulkTransactionStatusResponse {

    private int total;
    private int successCount;
    private int skippedCount;
    private int failedCount;

    private List<String> successTxnIds;
    private List<String> skippedTxnIds;
    private List<String> failedTxnIds;

    private List<String> messages;
}
