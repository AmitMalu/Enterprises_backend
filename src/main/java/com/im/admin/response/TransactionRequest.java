package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TransactionRequest {

    private String mobile;
    private String referenceId;
    private String beneId;
    private String txntype;     // IMPS | NEFT
    private String amount;
    private String pincode;
    private String address;
    private String dob;
    private String gstState;
    private String lat;
    private String lng;
    private String otp;
    private String stateresp;
}
