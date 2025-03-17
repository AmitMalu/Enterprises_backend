package com.im.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class VendorRoutingDto {

    @NotBlank(message = "Sub-product ID is required")
    private String subProductId;

    @NotBlank(message = "Sub-product name is required")
    private String subProductName;

    @NotBlank(message = "Vendor ID is required")
    private String vendorId;

    @NotBlank(message = "Vendor name is required")
    private String vendorName;

    @NotNull(message = "Priority is required")
    private Integer priority;

    @NotBlank(message = "Created by is required")
    private String createdBy;

    private String editedBy;


}
