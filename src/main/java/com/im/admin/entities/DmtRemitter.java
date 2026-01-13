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
}
