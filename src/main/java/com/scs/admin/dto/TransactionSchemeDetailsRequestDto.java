package com.scs.admin.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scs.admin.entities.SchemeRate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String transactionType;        //credit, debit , imps , neft
    private String baseTranxId;              //25041815234800204
    private String callbackUrl;
    private Double latitude;
    private Double longitude;
    private String vendorName;
    private String vendorTxnId;

    private String medium;
    private String clientIp;
    private String requestDomainName;

    private String mobileNumber;
    private String outletName;

    private String utrNo;


}
