package com.im.admin.entities.product;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
class ProductInfo {

    @NotNull(message = "Product name cannot be null")
    @NotBlank(message = "Product name cannot be blank")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "operator_source")
    private String operatorSource;

    @Column(name = "wallet_type")
    private String walletType;

    @NotNull(message = "Product code cannot be null")
    @NotBlank(message = "Product code cannot be blank")
    @Column(name = "product_code", nullable = false, unique = true)
    private String productCode;

    @Column(name = "product_code_for_scheme")
    private String productCodeForScheme;

    @NotNull(message = "Visibility status is required")
    @Column(name = "is_visible", nullable = false)
    private Boolean isVisible;

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
}

