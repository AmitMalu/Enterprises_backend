package com.scs.admin.dto;

import com.scs.admin.entities.vendor.VendorAccount;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VendorsDTO {

    private String id; // For update case

    @NotBlank(message = "Vendor Name is required")
    private String vendorName;

    @NotBlank(message = "Vendor Code is required")
    private String vendorCode;

    @NotNull(message = "Product ID is required")
    private Short productId;

    private String productName;

    private String modesSupported;
    private Boolean active;
    private String bankType;
    private String website;
    private String logo;
    private String parameter;

    private VendorAccount vendorAccount;

    private Boolean isWhiteLabelAdmin;
    private String whiteLabelAdminId;

    private String userField1;
    private String userField2;
    private String userField3;

    private String createdBy;
    private String editedBy;
    private LocalDateTime createdOn;
    private LocalDateTime editedOn;
}
