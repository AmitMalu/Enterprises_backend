package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
//@NoArgsConstructor
public class RemitterTransactionResponse {

    private Boolean status;
    private int response_code;
    private String ackno;
    private String utr;
    private Integer txn_status;
    private String benename;
    private String remarks;
    private String message;
    private String customercharge;
    private String gst;
    private String tds;
    private String netcommission;
    private String remitter;
    private String account_number;
    private String paysprint_share;
    private String txn_amount;
    private Double balance;
    private String referenceid;

//    // Optional custom constructor
//    public RemitterTransactionResponse(boolean status, String message, String ackno) {
//        this.status = status;
//        this.message = message;
//        this.ackno = ackno;
//    }
}
