package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RunPaisaCallback {

    private String status;              // STATUS
    private String statusCode;         // STATUS_CODE
    private String orderId;            // ORDER_ID
    private String txnMode;            // TXN_MODE
    private String txnAmount;          // TXN_AMOUNT
    private String cardCategory;       // CARD_CATEGORY
    private String txnDate;            // TXN_DATE (Consider converting to LocalDateTime if needed)
    private String txnInfo;            // TXN_INFO
    private String customerName;       // CUSTOMER_NAME
    private String customerEmail;      // CUSTOMER_EMAIL
    private String customerPhone;      // CUSTOMER_PHONE
    private String bankTxnId;          // BANK_TXNID
    private String bankCode;           // BANK_CODE
    private String errorId;            // ERROR_ID
    private String errorDesc;          // ERROR_DESC
    private String cardNumber;         // CARD_NUMBER (use masked or last 4 digits only)
    private String cardType;           // CARD_TYPE
    private String unmappedStatus;     // UNMAPPED_STATUS
    private String pgPartner;          // PG_PARTNER
    private String mercUnqRef;         // MERC_UNQ_REF
}
