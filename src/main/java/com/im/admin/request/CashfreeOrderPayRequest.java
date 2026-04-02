package com.im.admin.request;

import lombok.Data;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
public class CashfreeOrderPayRequest {

    private String paymentSessionId;

    private String paymentType; // CARD / UPI / NETBANKING

    // Card
    private String cardNumber;
    private String cardHolderName;
    private String expiryMonth;
    private String expiryYear;
    private String cvv;

    // UPI
    private String upiId;

    // Netbanking
    private String bankCode;
}
