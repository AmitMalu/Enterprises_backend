package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FundCreditDebitDto {

    private String fundingType;
    private String userAccountId;
    private String WalletType;
    private double amount;
    private double confirmAmount;
    private String narrationType;
    private String remarks;

}
