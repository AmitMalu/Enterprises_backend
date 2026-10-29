package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankVerificationRequest {

    private String id_number;    // Account number
    private String ifsc;         // IFSC code
    private boolean ifsc_details; // true/false
    private String schemeCode;
    private String subProductId;

}
