package com.im.admin.entities;

import com.im.admin.entities.vendor.Vendors;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "aeps_merchant_status")
public class AepsMerchantStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "retailer_id", nullable = false, unique = true)
    private String retailerId;

    @Column(name = "merchant_login_id", length = 100)
    private String merchantLoginId;

    @Column(name = "fingpay_primary_key")
    private Long fingpayPrimaryKey;

    @Column(name = "fingpay_txn_id", length = 200)
    private String fingpayTxnId;

    @Column(name = "onboarded", nullable = false)
    private Boolean onboarded = Boolean.FALSE;

    @Column(name = "ekyc_verified", nullable = false)
    private Boolean ekycVerified = Boolean.FALSE;

    @Column(name = "biometric_verified", nullable = false)
    private Boolean biometricVerified = Boolean.FALSE;

    @Column(name = "status", length = 20)
    private String status = "PENDING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendors vendor;

}
