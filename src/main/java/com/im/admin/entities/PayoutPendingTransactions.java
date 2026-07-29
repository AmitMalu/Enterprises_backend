package com.im.admin.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payout_pending_transactions")
@Entity
public class PayoutPendingTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String schemeCode;

    @Column(nullable = false)
    private String retailerId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String transactionType;  //IMPS/NEFT

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String ifscCode;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String beneficiaryName;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    private String medium;

    @Column(name = "temp_txn_id")
    private String tempTxnId;



}
