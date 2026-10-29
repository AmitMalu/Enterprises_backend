package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@Table(name = "unisus_payout_bank_list")
@Entity
public class UnisusPayoutBankList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "bank_id")
    private Integer bankId;

    @Column(name = "bank_code")
    private String bankCode;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "imps_enabled")
    private Integer impsEnabled;

    @Column(name = "neft_enabled")
    private Integer neftEnabled;

    @Column(name = "ac_verify_available")
    private Integer acVerifyAvailable;

    @Column(name = "status")
    private Integer status;
}
