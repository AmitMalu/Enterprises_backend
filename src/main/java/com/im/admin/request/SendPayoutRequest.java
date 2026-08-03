package com.im.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SendPayoutRequest {

    private String retailerId;
    private String accountNumber;
    private String ifscCode;
    private BigDecimal amount;
    private String orderId;
    private String beneficiaryName;    //card holder name.
    private String paymentMode;
    private String subProductId;
    private String callbackUrl;
    private String schemeCode;
    private String channelType;
    private String latitude;
    private String longitude;

    private String emailId;

    private String bankName;      //To transfer bank name.
    private String schemeName;
    private String partnerId;

    private String medium;
    private String clientIp;
    private String requestDomainName;

    private String imSubProductName;


}
