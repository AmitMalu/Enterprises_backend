package com.scs.admin.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "bbps_pending_transactions")
@Entity
public class BBPSPendingTransactions {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String retailerId;

    @Column(nullable = false)
    private String walletType;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String vendorBaseUrl;

    @Column(nullable = false)
    private String token;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String clientSecret;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "scheme_rate_id", nullable = false)
    private SchemeRate schemeRate;

    @Column(nullable = false)
    private String operatorName;

    @Column(nullable = false)
    private String gst;

    @Column(nullable = false)
    private String tds;

    @Column(nullable = false)
    private String subProductName;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String actualVendorStatus;

    @Column(nullable = false)
    private String consumerNumber;


}
