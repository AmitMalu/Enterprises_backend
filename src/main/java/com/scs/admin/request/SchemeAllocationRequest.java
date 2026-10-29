package com.scs.admin.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class SchemeAllocationRequest {

    @NotBlank(message = "Hierarchy ID is required")
    private String hierarchyId;

    @NotBlank(message = "Product is required")
    private String productCode;

    @NotBlank(message = "Sub-product is required")
    private String subProductCode;

    @NotBlank(message = "Scheme is required")
    private String schemeCode;
}

