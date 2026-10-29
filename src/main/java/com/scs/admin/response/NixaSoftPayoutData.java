package com.scs.admin.response;

/**
 * @author SHUBHAM KHOPADE
 */
import lombok.Data;

@Data
public class NixaSoftPayoutData {

    private String requestId;
    private String apiTxnId;
    private String utr;
    private String amount;
    private String charge;
    private String opening_balance;
    private String closing_balance;
    private String beneficiaryName;
    private String bankName;
    private String accountNumber;
    private String ifscCode;
    private String description;
}
