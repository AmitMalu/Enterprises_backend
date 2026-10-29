package com.scs.admin.service;

import com.scs.admin.response.AuthorizationResponse;

/**
 * VidualPay vendor service interface.
 *
 * @author SHUBHAM KHOPADE
 */
public interface VidualPayService {

    /**
     * Calls VidualPay /payoutapi/api/signature/authorizeuat.
     * Credentials are fetched from the DB by vendorCredentialsId.
     * Returns the decrypted authorization token in {@link AuthorizationResponse#getDecryptedData()}.
     *
     * @param vendorCredentialsId PK of the vendor_credentials row for VidualPay
     */
    AuthorizationResponse authorize(Long vendorCredentialsId);
}
