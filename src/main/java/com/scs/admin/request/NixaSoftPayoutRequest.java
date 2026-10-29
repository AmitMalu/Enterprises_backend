package com.scs.admin.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NixaSoftPayoutRequest {

    private String amount;
    private String mobileNumber;
    private String requestId;
    private String accountNumber;
    private String ifscCode;
    private String beneficiaryName;
    private String bankName;
    private String transferMode;
    private String emailId;
    private String latitude;
    private String longitude;
}
