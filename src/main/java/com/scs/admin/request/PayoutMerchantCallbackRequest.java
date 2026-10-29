package com.scs.admin.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PayoutMerchantCallbackRequest {

    @NotBlank(message = "Callback URL is required")
    private String callbackUrl;

}
