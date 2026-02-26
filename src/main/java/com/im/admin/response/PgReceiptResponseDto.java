package com.im.admin.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PgReceiptResponseDto {

    private String dateTime;     // TXN_DATE
    private String txnId;        // Your system txnId
    private String orderId;      // PG order id
    private String number;       // CUSTOMER_PHONE
    private Long txnAmount;      // TXN_AMOUNT (no decimal)
    private String paymentMode;  // CARD_CATEGORY (VISA / MASTER / etc.)
    private String bankName;     // From card_type_master issuerName
    private String bankRefNo;    // BANK_TXNID
    private String txnStatus;
}
