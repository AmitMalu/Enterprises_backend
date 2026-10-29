package com.scs.admin.request;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.Data;

@Data
public class NixaSoftPayoutStatusResponse {

    private String status;
    private String service;
    private String requestId;
    private String orderId;
    private String amount;
    private String charges;
    private String utr;
    private String beneficiaryName;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String mobileNumber;
    private String description;

}
