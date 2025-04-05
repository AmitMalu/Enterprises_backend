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
    private String hierarchyId;           // RetailerId
    private Double amount;
    private Double confirmAmount;
    private String operatorCode;
    private String operatorName;

    private String walletType;           //Prepaid, Postpaid, etc
    private SchemeRate schemeRate;       //This amount we get from another api for calculation
    private String gstValue;             //gst
    private String tdsRate;             //tds

    private Boolean hasCharge;
    private Boolean isCommission;
    private String transactionType;        //credit, debit

}
