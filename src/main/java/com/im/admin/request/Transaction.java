package com.im.admin.request;

import lombok.Data;

@Data
public class Transaction {

    private Integer ServiceType;
    private String ServiceCode;
    private String ServiceName;

    private String CustomerNo;
    private String CustomerMobile;
    private Double Amount;

    private Integer DiscOrCharge;
    private Double DiscOrChargePer;
    private Double DiscOrChargeRs;
    private Double DiscTDS;

    private String TrnRefNo;
    private String OperatorID;
    private String TransactionID;

    private Integer Status;
    private String StatusMsg;
}
