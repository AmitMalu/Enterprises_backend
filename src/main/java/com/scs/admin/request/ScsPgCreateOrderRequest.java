package com.scs.admin.request;

import lombok.Getter;
import lombok.Setter;

/**
 * @author SHUBHAM KHOPADE
 */

@Getter
@Setter
public class ScsPgCreateOrderRequest {

    private String geoLocation;
    private String userId;
    private String clientIp;
    private String subProductName;
    private String scsSubProductName;
    private String amount;
    private String medium;
    private String holderMobileNumber;
    private String cardHolderName;
    private String paymentMode;

    private String subProductId;
    private String initiatedBy;


}
