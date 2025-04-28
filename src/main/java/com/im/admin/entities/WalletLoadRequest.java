package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_load_request")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WalletLoadRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "hierarchy_id", nullable = false)
    private String hierarchyId;

    @Column(name = "mobile_number", nullable = false)
    private String mobileNumber;

    @Column(name = "parent_hierarchy_id")
    private String parentHierarchyId;

    @Column(name = "deposit_amount", nullable = false)
    private Double depositAmount;

    @Column(name = "deposit_image")
    private String depositImage;

    @Column(name = "deposit_date")
    private LocalDate depositDate;

    @Column(name = "deposit_time")
    private LocalTime depositTime;

    @Column(name = "approve_or_reject_date")
    private LocalDate approveOrRejectDate;

    @Column(name = "approve_or_reject_time")
    private LocalTime approveOrRejectTime;

    @Column(nullable = false)
    private String status = "Pending";

    @CreationTimestamp
    @Column(name = "created_datetime", nullable = false, updatable = false)
    private LocalDateTime createDateTime;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_name")
    private String bankName;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "bank_tran_id")
    private String bankTranId;

    @Column(name = "payment_mode")
    private String paymentMode;
}

