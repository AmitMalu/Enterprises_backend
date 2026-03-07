package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "dmt_bank_list")
public class DmtBankList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "bank_id")
    private Integer BankId;

    @Column(name = "bank_name")
    private String BankName;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "unisus_mifsc_code")
    private String unisusMIfcsCode;

    @Column(name = "unisus_bank_id")
    private Integer unisusBankId;


}
