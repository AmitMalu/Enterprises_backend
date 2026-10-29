package com.scs.admin.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.scs.admin.entities.enums.WalletType;
import com.scs.admin.entities.product.Products;
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

    @Column(name = "description")
    private String description;

    @Column(name = "wallet_type")
    private WalletType walletType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @JoinColumn(name = "product_id", referencedColumnName = "id")
    private Products product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subProduct_id", referencedColumnName = "id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private SubProducts subProduct;

    @Column(name = "payment_currency")
    private String paymentCurrency;

    @Column(name = "is_allow_per_day_card_limit")
    private Boolean isAllowPerDayCardLimit;

    @Column(name = "per_day_transaction_limit")
    private String PerDayTransactionLimit;

    @Column(name = "is_allow_monthly_card_limit")
    private Boolean isAllowMonthlyCardLimit;

    @Column(name = "monthly_transaction_limit")
    private String MonthlyTransactionLimit;

    @Column(name = "check_amount_pattern")
    private Boolean checkAmountPattern;

    @Column(name = "max_amount")
    private BigDecimal maxAmount;

    @Column(name = "min_amount")
    private BigDecimal minAmount;

    @Column(name = "blocked")
    private Boolean blocked;

    @Column(name = "created_on")
    private LocalDateTime createdOn;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;

}

