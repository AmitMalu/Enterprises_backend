package com.scs.admin.dto;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FetchBillDetailsResponseDTO {

    private int status;
    private String message;

    @JsonProperty("billInfo")
    private Map<String, Object> billInfo;

    private String serviceName;
    private int isAmountEditable;

    @JsonProperty("is_allowed_to_pay_gt_bill_amount")
    private String isAllowedToPayGtBillAmount;


}


