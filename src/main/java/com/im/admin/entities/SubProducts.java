package com.im.admin.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
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

    @Column(name = "product", nullable = false)
    @NotNull(message = "Product Code cannot be null")
    @NotBlank(message = "Product Code cannot be blank")
    private String productCode;

    @Column(name = "product_id")
    @NotNull(message = "Product Id cannot be null")
    private Short productId;

    @Column(name = "is_settlement", nullable = false)
    @NotNull(message = "Is Settlement is required")
    private Boolean isSettlement;

    @Column(name = "is_active", nullable = false)
    @NotNull(message = "Is Active is required")
    private Boolean isActive = false;

    @Column(name = "enabled")
    @NotNull(message = "Is Enabled is required")
    private Boolean enabled;

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
