package com.scs.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BillRechargeResponseDTO {

    private String status; // "SUCCESS", "FAILURE", "PENDING", "ACCEPTED"
    private String message;
    private String txnId;
    private String txnTime;
    private String operator_ref;
    private String billerName;
    private String providerName;
    private BigDecimal amount;
    private Integer statusId;

}

