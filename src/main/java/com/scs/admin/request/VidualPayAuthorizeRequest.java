package com.scs.admin.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for the VidualPay partner authorization API.
 * The caller provides the vendor credentials record ID (PK from vendor_credentials table).
 * All four header values (secretKey, saltKey, encryptdecryptKey, userId)
 * are looked up from that record — the frontend sends nothing sensitive.
 *
 * @author SHUBHAM KHOPADE
 */
@Data
public class VidualPayAuthorizeRequest {

    @NotNull(message = "vendorCredentialsId is required")
    private Long vendorCredentialsId;
}
