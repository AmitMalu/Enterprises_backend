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

    @Column(nullable = false)
    private String transactionId;

    @Column(nullable = false)
    private String schemeCode;

    @Column(unique = true, nullable = false)
    private String transactionRef;

    private String operatorCode;

    private String customerNumber;

    private BigDecimal amount;

    private String rechargeType;

    private String vendorResponse;

    private String status; // PENDING / SUCCESS / FAILED / HOLD / REFUNDED

    private LocalDateTime createdAt;

//    @ManyToOne
//    @JoinColumn(name = "retailer_id")
//    private Retailer retailer;

    @ManyToOne
    @JoinColumn(name = "vendor_id")
    private Vendors vendor;


    @Column(nullable = false)
    private String transactionType;

    @Column(nullable = false)
    private String retailerId;

    @Column(nullable = false)
    private String VendorTransactionId;

    @Column(nullable = false)
    private String OperatorTransactionId;





}

