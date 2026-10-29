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
public class BillRechargeRequestDTO {

    private String api_token;
    private Integer userId;
    private String secretKey;
    private String number;
    private Integer provider;
    private BigDecimal amount;
    private String clientId;
    private String context;
    private String bill_due_date;
    private String consumerNumber;
    private String dob;
    private String field_one;
    private String field_two;
    private String field_three;
    private String field_four;

    private String retailerId;
    private String subProductId;

    private String medium;
    private String requestDomainName;

}

