package com.scs.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentGatewayCardBinRequestDTO {

    private String cardBinNo;
    private String productCode;
    private String subProductCode;
    private String schemeCode;
    private String agentId;

}
