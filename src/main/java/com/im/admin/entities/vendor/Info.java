package com.im.admin.entities.vendor;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Info {

    @NotNull(message = "Product Code is required")
    private String productCode;

    @NotNull(message = "Sub Product is required")
    private String subProduct;

    @NotNull(message = "API Vendor Name is required")
    private String apiVendorName;

    @NotNull(message = "API Vendor Class is required")
    private String apiVendorClass;

    @NotNull(message = "API Vendor Slug is required")
    private String apiVendorSlug;

    @NotNull(message = "Priority is required")
    private Integer priority;

    private String modesSupported;

    @NotNull(message = "Active status is required")
    private Boolean active;

    private String bankType;

    @Column(name = "website")
    private String website;

    @Column(name = "logo")
    private String logo;

    @Column(name = "parameter")
    private String parameter;

}
