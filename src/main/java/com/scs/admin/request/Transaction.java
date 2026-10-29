package com.scs.admin.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Transaction {

    @JsonProperty("ServiceType")
    private Integer serviceType;

    @JsonProperty("ServiceCode")
    private String serviceCode;

    @JsonProperty("ServiceName")
    private String serviceName;

    @JsonProperty("CustomerNo")
    private String customerNo;

    @JsonProperty("CustomerMobile")
    private String customerMobile;

    @JsonProperty("Amount")
    private Double amount;

    @JsonProperty("DiscOrCharge")
    private Integer discOrCharge;

    @JsonProperty("DiscOrChargePer")
    private Double discOrChargePer;

    @JsonProperty("DiscOrChargeRs")
    private Double discOrChargeRs;

    @JsonProperty("DiscTDS")
    private Double discTDS;

    @JsonProperty("TrnRefNo")
    private String trnRefNo;

    @JsonProperty("OperatorID")
    private String operatorID;

    @JsonProperty("TransactionID")
    private String transactionID;

    @JsonProperty("Status")
    private Integer status;

    @JsonProperty("StatusMsg")
    private String statusMsg;
}
