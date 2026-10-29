package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import com.scs.admin.entities.vendor.Vendors;

import java.time.LocalDateTime;

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
    private String beneId;

    @Column(name = "name")
    private String name;

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

    @Column(name = "dob")
    private String dob;

    @Column(name = "address")
    private String address;

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "gst_state")
    private String gst_state;

    @Column(name = "reference_key")
    private String referenceKey;


    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;


}

