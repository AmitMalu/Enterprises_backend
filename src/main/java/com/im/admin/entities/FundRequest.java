package com.im.admin.entities;

import com.im.admin.entities.enums.Status;
import com.im.admin.entities.enums.WalletType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "fund_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FundRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "requester_hierarchyId", nullable = false)
    private String requesterHierarchyId;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal requestedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "wallet_type", nullable = false)
    private WalletType walletType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.PENDING;

    @Column(name = "approve_or_reject_hierarchyId")
    private String approveOrRejectHierarchyId;

    @CreationTimestamp
    @Column(name = "request_raise_date", updatable = false)
    private LocalDate requestRaiseDate;

    @CreationTimestamp
    @Column(name = "request_raise_time", updatable = false)
    private LocalTime requestRaiseTime;

    @Column(name = "approve_or_reject_date")
    private LocalDate approveOrRejectDate;

    @Column(name = "approve_or_reject_time")
    private LocalTime approveOrRejectTime;

    @Column(name = "remarks")
    private String remarks;
}

