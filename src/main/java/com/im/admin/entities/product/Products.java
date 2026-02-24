package com.im.admin.entities.product;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Table(name = "products")
public class Products {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Uses MySQL auto-increment
    @Column(name = "id", columnDefinition = "TINYINT UNSIGNED")
    private Short id;

    @NotNull(message = "Product name cannot be null")
    @NotBlank(message = "Product name cannot be blank")
    @Column(name = "name", nullable = false,unique = true)
    private String name;

    @Builder.Default
    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode = "1";

    @Column(name = "product_code_for_scheme")
    private String productCodeForScheme;

    @NotNull(message = "Visibility status is required")
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;

    @Column(name = "transaction_type")
    private String transactionType;

    @Column(name = "is_commissions")
    private Boolean isCommissions;

    @Column(name = "select_is_commission")
    private String selectIsCommission;

    @NotNull(message = "Active status is required")
    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "dashboard_url")
    private String dashboardUrl;

    @Column(name = "dashboard_url_for_apk")
    private String dashboardUrlForApk;

    @Column(name = "dashboard_image")
    private String dashboardImage;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "product_group")
    private String productGroup;

    @Column(name = "reverse_tx")
    private Boolean reverseTx;

    @Column(name = "cluster_mode")
    private Boolean clusterMode;

    @Column(name = "is_settlement")
    private Boolean isSettlement;

    @Column(name = "has_charges")
    private Boolean hasCharges;

    @Column(name = "blocked")
    private Boolean blocked;

    @Embedded
    @Valid
    private Inventory inventory;

    @Embedded
    @Valid
    private Cluster cluster;

    //Account related Columns
    @NotNull(message = "Minimum postpaid balance is required")
    @Column(name = "minimum_postpaid_balance", nullable = false)
    private Double minimumPostpaidBalance;

    @NotNull(message = "Minimum prepaid balance is required")
    @Column(name = "minimum_prepaid_balance", nullable = false)
    private Double minimumPrepaidBalance;

    @NotNull(message = "Minimum Gullak balance is required")
    @Column(name = "minimum_gullak_balance", nullable = false)
    private Double minimumGullakBalance;

    @NotNull(message = "Minimum single transaction amount is required")
    @Column(name = "minimum_single_transaction_amount", nullable = false)
    private Double minimumSingleTransactionAmount;

    @NotNull(message = "Maximum single transaction amount is required")
    @Column(name = "maximum_single_transaction_amount", nullable = false)
    private Double maximumSingleTransactionAmount;

    @NotNull(message = "SGST is required")
    @Column(name = "sgst", nullable = false)
    private Double sgst;

    @NotNull(message = "TDS is required")
    @Column(name = "tds", nullable = false)
    private Double tds;

    @Column(name = "user_field_1")
    private String userField1;

    @Column(name = "user_field_2")
    private String userField2;

    @Column(name = "user_field_3")
    private String userField3;

    @CreationTimestamp
    @Column(name = "created_on", nullable = false, updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;
}