package com.im.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RemitterTransactionResponse {

    private Boolean status;           // true or false
    private int response_code;        // vendor response code
    private String ackno;             // vendor acknowledgement number
    private String utr;               // bank UTR number
    private Integer txn_status;       // transaction status code
    private String benename;          // beneficiary name
    private String remarks;           // vendor remarks
    private String message;           // message or description
    private String customercharge;    // customer charge
    private String gst;               // GST value
    private String tds;               // TDS value
    private String netcommission;     // net commission
    private String remitter;          // remitter mobile or id
    private String account_number;    // beneficiary account number
    private String paysprint_share;   // vendor share
    private String txn_amount;        // transaction amount
    private Double balance;           // updated wallet balance
    private Long referenceid;         // vendor reference id


    public RemitterTransactionResponse(boolean status, String message, String ackno) {
        try {
            this.status = status;
            this.message = message;
            this.ackno = ackno;
        } catch (Exception e) {
            this.status = false;
            this.message = "Error while creating RemitterTransactionResponse: " + e.getMessage();
            this.ackno = null;
        }
    }
}
