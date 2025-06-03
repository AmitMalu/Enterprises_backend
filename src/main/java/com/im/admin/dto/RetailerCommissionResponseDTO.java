package com.im.admin.dto;


import com.im.admin.entities.enums.RateType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RetailerCommissionResponseDTO {

    private String cardType;
    private String operatorCode;
    private String schemeCode;
    private Double retailerCommission;
    private RateType rateType;

}
