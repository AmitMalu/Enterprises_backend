package com.scs.admin.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request payload for updating lien (cutoff) amounts on a hierarchy wallet.
 */
@Data
public class UpdateLienAmountRequest {

    @NotBlank(message = "hierarchyId is required")
    private String hierarchyId;

    @NotNull(message = "prepaidCutoffAmount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "prepaidCutoffAmount must be >= 0")
    private BigDecimal prepaidCutoffAmount;

    @NotNull(message = "postpaidCutoffAmount is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "postpaidCutoffAmount must be >= 0")
    private BigDecimal postpaidCutoffAmount;

    private String remark;
}
