package com.im.admin.entities;

import com.im.admin.entities.vendor.Vendors;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "dmt_remmiter")
public class DmtRemitter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mobile", unique = true, nullable = false)
    private String mobile;
    private String aadhaar;

    private String ekycId;
    private String stateResp;

    @Column(name = "limit_amount")
    private Double limitAmount;

    @Column(name = "name")
    private String name;

    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_generate_at")
    private LocalDateTime otpGeneratedAt;


    @Column(columnDefinition = "VARCHAR(20) DEFAULT 'PENDING'")
    private String status; // PENDING / REGISTERED

    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id", nullable = false)
    private Vendors vendor;

    @Column(name = "vendor1_used_limit")
    private Double vendor1UsedLimit = 0.0;

    @Column(name = "vendor2_used_limit")
    private Double vendor2UsedLimit = 0.0;

    @Column(name = "vendor3_used_limit")
    private Double vendor3UsedLimit = 0.0;

    @Column(name = "reference_key")
    private String referenceKey;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "otp_reference")
    private String otpReference;

    public double getUsedLimitForVendor(String vendorId) {

        if ("InstantPay DMT".equalsIgnoreCase(vendorId)) {
            return vendor1UsedLimit != null ? vendor1UsedLimit : 0.0;

        } else if ("FingPay".equalsIgnoreCase(vendorId)) {
            return vendor2UsedLimit != null ? vendor2UsedLimit : 0.0;

        } else if ("Paysprint DMT".equalsIgnoreCase(vendorId)) {
            return vendor3UsedLimit != null ? vendor3UsedLimit : 0.0;
        }

        return 0.0;
    }

    public void addUsedLimit(String vendorId, double amount) {

        if ("InstantPay DMT".equalsIgnoreCase(vendorId)) {
            this.vendor1UsedLimit = (this.vendor1UsedLimit == null ? 0.0 : this.vendor1UsedLimit) + amount;

        } else if ("FingPay".equalsIgnoreCase(vendorId)) {
            this.vendor2UsedLimit = (this.vendor2UsedLimit == null ? 0.0 : this.vendor2UsedLimit) + amount;

        } else if ("Paysprint DMT".equalsIgnoreCase(vendorId)) {
            this.vendor3UsedLimit = (this.vendor3UsedLimit == null ? 0.0 : this.vendor3UsedLimit) + amount;
        }
    }

    public void resetMonthlyLimits() {

        this.vendor1UsedLimit = 0.0;
        this.vendor2UsedLimit = 0.0;
        this.vendor3UsedLimit = 0.0;

        this.updatedAt = LocalDateTime.now();
    }


}
