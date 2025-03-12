package com.im.admin.entities.vendor;

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

    @NotBlank(message = "Sub Product Code is required")
    @Column(name = "sub_product_code")
    private String subProductCode;

    @NotBlank(message = "Sub Product Id is required")
    @Column(name = "sub_product_Id")
    private String subProductId;

    @NotBlank(message = "Vendor Name is required")
    private String vendorName;

    @NotBlank(message = "Vendor Code is required")
    private String vendorCode;

    @NotNull(message = "Priority is required")
    private Integer priority;

    private String modesSupported;

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
