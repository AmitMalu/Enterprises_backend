package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.im.admin.entities.vendor.Vendors;

@Entity
@Getter
@Setter
@Table(name = "dmt_beneficiary")
public class DmtBeneficiary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "remitter_id", nullable = false)
    private DmtRemitter remitter;

    @Column(name = "bene_id")
    private Long beneId;

    @Column(name = "bene_name")
    private String beneName;

    @Column(name = "bank_id")
    private String bankId;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "account_no")
    private String accountNo;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "verified")
    private String verified;

    @Column(name = "status")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendors vendor;

}

