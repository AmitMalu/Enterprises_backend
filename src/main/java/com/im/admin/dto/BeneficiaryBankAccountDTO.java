package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BeneficiaryBankAccountDTO {

    private String retailerId;
    private String bankName;
    private String bankHolderName;
    private String accountNumber;
    private String ifscCode;
    private String createdBy;
}
