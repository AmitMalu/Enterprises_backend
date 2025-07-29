package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Table(name = "beneficiary_bank_accounts")
@Entity
public class BeneficiaryBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retailer_id")
    private String retailerId;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "bank_holder_name")
    private String bankHolderName;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "ifsc_code")
    private String ifscCode;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

//    @Column(name = "is_deleted")
//    private Boolean isDeleted;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

}
