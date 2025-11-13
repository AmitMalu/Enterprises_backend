package com.im.admin.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class PaysprintPennyDropResponse {

    private boolean status;
    private int response_code;
    private String utr;
    private String ackno;
    private int txn_status;
    private String benename;
    private String message;
    private Double balance;
    private String refid;
}
