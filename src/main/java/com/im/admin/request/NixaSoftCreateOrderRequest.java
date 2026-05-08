package com.im.admin.request;

import lombok.Data;

import java.math.BigDecimal;

/**
 * @author SHUBHAM KHOPADE
 */
@Data
public class NixaSoftCreateOrderRequest {

    private String beneficiaryName;
    private String mobileNo;
    private String emailId;
    private String amount;
    private String redirectUrl;
    private String requestId;
    private Short productId;
    private String subProductId;
    private String initiatedBy;
    private String cardBinNo;

}
