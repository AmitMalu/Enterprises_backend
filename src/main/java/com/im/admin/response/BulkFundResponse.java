package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkFundResponse {

    private String userAccountId;   // From Excel
    private String walletType;      // PREPAID / POSTPAID
    private String fundingType;     // CREDIT / DEBIT
    private BigDecimal amount;      // Entered amount
    private BigDecimal confirmAmount; // Confirmed amount (for validation)

    private String status;          // SUCCESS / FAILED
    private String message;         // Success/Failure reason

    private String transactionId;   // Generated transaction reference
    private Integer rowNumber;      // Excel row number (useful for debugging)
}


