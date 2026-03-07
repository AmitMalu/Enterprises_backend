package com.im.admin.entities;

import com.im.admin.entities.vendor.Vendors;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Entity
@Table(name = "recharge_transactions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RechargeTransaction extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false)
    private String transactionId;

    @Column(name = "scheme_code", nullable = false)
    private String schemeCode;

    @Column(name = "transaction_ref", unique = true)
    private String transactionRef;

    @Column(name = "operator_code")
    private String operatorCode;

    @Column(name = "customer_number")
    private String customerNumber;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "recharge_type")
    private String rechargeType;

    @Column(name = "vendor_response")
    private String vendorResponse;

    @Column(name = "status")
    private String status;   // PENDING / SUCCESS / FAILED / HOLD / REFUNDED

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendors vendor;

    @Column(name = "transaction_type", nullable = false)
    private String transactionType;

    @Column(name = "retailer_id")
    private String retailerId;

    @Column(name = "vendor_transaction_id")
    private String vendorTransactionId;

    @Column(name = "operator_transaction_id")
    private String operatorTransactionId;


}

