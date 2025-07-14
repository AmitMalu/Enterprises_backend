package com.im.admin.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BankDetailsDTO {

    private String bankName;
    private String bankAccountNumber;
    private String bankHolderName;
    private String ifscCode;
    private Boolean isVerified;

}

