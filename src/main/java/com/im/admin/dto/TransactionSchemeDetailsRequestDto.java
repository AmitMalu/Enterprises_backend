package com.im.admin.dto;


import com.im.admin.entities.SchemeRate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TransactionSchemeDetailsRequestDto {

    private String productCode;
    private String subProductCode;
    private String hierarchyId;           // SuperDistributorId, PartnerId, DistributorId, RetailerId, etc
    private Double amount;
    private Double confirmAmount;
    private String operatorCode;
    private String operatorName;

    private String walletType;           //Prepaid, Postpaid, etc
    private SchemeRate schemeRate;       //This amount we get from another api for calculation
    private String taxValue;             //gst
    private String tdsRate;             //tds

}
