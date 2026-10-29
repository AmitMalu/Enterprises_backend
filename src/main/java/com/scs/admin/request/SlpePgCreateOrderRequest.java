package com.scs.admin.request;

import lombok.Data;

/**
 * @author SHUBHAM KHOPADE
 */

@Data
public class SlpePgCreateOrderRequest {

    private String initiatedBy;
    private Short productId;
    private String subProductId;
    private Double amount;
    private String customerName;
    private String customerMobile;
    private String customerEmail;
    private String paymentMode;

}
