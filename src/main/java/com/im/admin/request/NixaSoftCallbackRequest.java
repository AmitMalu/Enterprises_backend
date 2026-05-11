package com.im.admin.request;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.Data;

@Data
public class NixaSoftCallbackRequest {

    private String status;

    private String requestId;

    private String orderId;

    private String amount;

    private String charges;

    private String settlement_amount;

    private String utr;

    private String payment_mode;

    private String network;

    private String card_last4;

    private String subtype;

    private String bank_name;

    private String beneficiaryName;

    private String emailId;

    private String mobileNumber;

    private String description;

    private String product_name;
}
