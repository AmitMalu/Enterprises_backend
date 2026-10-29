package com.scs.admin.request;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSettlementStatusRequest {

    private List<String> transactionIds;
    private String walletType;      // "PREPAID" or "POSTPAID"
    private String newStatus;       // "SUCCESS" or "FAILURE"

}

