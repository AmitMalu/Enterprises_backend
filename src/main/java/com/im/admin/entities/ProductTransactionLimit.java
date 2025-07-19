package com.im.admin.entities;

import com.im.admin.entities.product.Products;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "product_transaction_limit")
public class ProductTransactionLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Products product;

    // Daily limit
    @Column(name = "daily_transaction_limit", nullable = false)
    private Integer dailyTransactionLimit;

    // Monthly limit
    @Column(name = "monthly_transaction_limit", nullable = false)
    private Integer monthlyTransactionLimit;

    // Yearly limit
    @Column(name = "yearly_transaction_limit", nullable = false)
    private Integer yearlyTransactionLimit;

    // Max amount allowed per transaction
    @Column(name = "max_per_transaction_amt", nullable = false, precision = 15, scale = 2)
    private BigDecimal maxPerTransactionAmt;
}

