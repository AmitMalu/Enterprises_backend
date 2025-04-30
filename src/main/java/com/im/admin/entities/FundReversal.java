package com.im.admin.entities;

import com.im.admin.entities.enums.ConstantStatus;
import com.im.admin.entities.enums.WalletType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@Table(name = "fund_reversal")
public class FundReversal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "parent_hierarchy_id")
    private String parentHierarchyId;     // who is fetching

    @Column(name = "child_hierarchy_id")
    private String childHierarchyId;      // from whom fetching

    @Column(name = "reversal_amount")
    private BigDecimal reversalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type")
    private WalletType walletType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ConstantStatus status = ConstantStatus.PENDING;

    @CreationTimestamp
    @Column(name = "request_date", updatable = false)
    private LocalDate requestDate;

    @CreationTimestamp
    @Column(name = "request_time", updatable = false)
    private LocalTime requestTime;

    @Column(name = "action_date")
    private LocalDate actionDate;

    @Column(name = "action_time")
    private LocalTime actionTime;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "otp")
    private String otp;

    @Column(name = "otp_generated_time")
    private LocalDateTime otpGeneratedTime;

    @Column(name = "otp_verified")
    private Boolean otpVerified = false;

}

