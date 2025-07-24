package com.im.admin.entities.vendor;

import com.im.admin.entities.product.Products;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
@Table(name = "vendors")
public class Vendors {

    @Id
    @Column(name = "id", columnDefinition = "CHAR(2)")
    private String id;

    @NotBlank(message = "Vendor Name is required")
    private String vendorName;

    @NotBlank(message = "Vendor Code is required")
    private String vendorCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @NotNull(message = "Product is required")
    private Products product;

    private String modesSupported;

    @Builder.Default
    @NotNull(message = "Active status is required")
    private Boolean active = false;

    private String bankType;

    @Column(name = "website")
    private String website;

    @Column(name = "logo")
    private String logo;

    @Column(name = "parameter")
    private String parameter;

    @Embedded
    @Valid
    private VendorAccount vendorAccount;

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

    @Column(name = "created_by", nullable = false, updatable = false)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "edited_on")
    private LocalDateTime editedOn;

    @Column(name = "edited_by")
    private String editedBy;
}
