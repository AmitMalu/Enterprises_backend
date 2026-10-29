package com.scs.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FundTransactionResult {

    private int rowNumber;        // Excel row number (for easy tracing)
    private String userAccountId; // User ID on which operation was performed
    private String walletType;    // Wallet type (PREPAID/POSTPAID etc.)
    private String transactionType; // CREDIT or DEBIT
    private Double amount;        // Transaction amount
    private String transactionId; // Generated transaction ID
    private String status;        // SUCCESS or FAILURE
    private String message;       // Error or success message
}

