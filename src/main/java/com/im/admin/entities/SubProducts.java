package com.im.admin.entities;

import com.im.admin.entities.product.Products;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "sub_products")
public class SubProducts {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(2)")
    private String id;  // Stores 01 to 99 as a string

    @Column(name = "sub_product_title", nullable = false)
    private String subProductTitle;

    @Column(name = "sub_product_code", nullable = false, unique = true)
    @NotNull(message = "Sub Product Code cannot be null")
    @NotBlank(message = "Sub Product Code cannot be blank")
    private String subProductCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Products product;

    @Column(name = "wallet_type")
    private String walletType;

    @Column(name = "is_settlement")
    private Boolean isSettlement;

    @Column(name = "settlement_time")
    private String settlementTime;

    @Column(name = "is_active", nullable = false)
    @NotNull(message = "Is Active is required")
    private Boolean isActive = false;

    @Column(name = "is_visible")
    @NotNull(message = "Is Visible is required")
    private Boolean isVisible;

    @Column(name = "is_blocked")
    @NotNull(message = "Is Blocked is required")
    private Boolean isBlocked;

    @Column(name = "is_white_label_admin")
    private Boolean isWhiteLabelAdmin;

    @Column(name = "white_label_admin_id")
    private String whiteLabelAdminId;

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
