package com.im.admin.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Embeddable
public class BankDetails {

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "IFSC_code")
    private String ifscCode;

    @Column(name = "branch_name")
    private String branchName;

}
