package com.im.admin.entities;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "vendor_credentials")
public class VendorCredentials {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_id", nullable = false)
    @NotNull(message = "vendor is required")
    private String vendorId;

    @Column(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Short productId;

    @Column(name = "sub_product_id", nullable = false)
    @NotNull(message = "Sub Product is required")
    private String subProductId;

    @Column(name = "base_url_uat")
    private String baseUrlUat;

    @Column(name = "base_url_prod")
    private String baseUrlProd;

    @Column(name = "client_key")
    private String clientKey;

    @Column(name = "client_secret")
    private String clientSecret;

    @Column(name = "username")
    private String username;

    @Column(name = "password")
    private String password;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @CreationTimestamp
    @Column(name = "created_on", updatable = false)
    private LocalDateTime createdOn;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;
}

