package com.im.admin.service.impl;

import com.im.admin.commons.CommonFunctions;
import com.im.admin.entities.VendorCredentials;
import com.im.admin.repositories.VendorCredentialsRepository;
import com.im.admin.response.AuthorizationResponse;
import com.im.admin.response.VidualPayResponse;
import com.im.admin.service.VidualPayService;
import com.im.admin.util.VidualPayCryptoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;

/**
 * VidualPay partner authorization service.
 *
 * Header mapping for POST /payoutapi/api/signature/authorizeuat:
 *   secretKey         → vendorCreds.clientSecret
 *   saltKey           → vendorCreds.saltKey
 *   encryptdecryptKey → vendorCreds.clientId
 *   userId            → vendorCreds.username
 *
 * The encrypted data in the response is decrypted using:
 *   edKey = encryptdecryptKey (vendorCreds.clientId)
 *   ivKey = saltKey           (vendorCreds.saltKey)
 *
 * @author SHUBHAM KHOPADE
 */
@Service
@Slf4j
public class VidualPayServiceImpl implements VidualPayService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VendorCredentialsRepository vendorCredentialsRepository;

    @Autowired
    private CommonFunctions commonFunctions;

    @Override
    public AuthorizationResponse authorize(Long vendorCredentialsId) {

        LocalDateTime requestDateTime = LocalDateTime.now();
        String request  = "VidualPay authorize — vendorCredentialsId=" + vendorCredentialsId;
        String response = null;
        LocalDateTime responseDateTime = null;

        try {
            // ── 1. Fetch credentials from DB ───────────────────────────────
            VendorCredentials creds = vendorCredentialsRepository
                    .findById(vendorCredentialsId)
                    .orElseThrow(() -> new RuntimeException(
                            "VendorCredentials not found for id: " + vendorCredentialsId));

            if (!Boolean.TRUE.equals(creds.getIsActive())) {
                throw new RuntimeException(
                        "VendorCredentials id=" + vendorCredentialsId + " is not active");
            }

            // ── 2. Build the VidualPay crypto util with this record's keys ─
            //    edKey = encryptdecryptKey = clientId
            //    ivKey = saltKey
            VidualPayCryptoUtil crypto = new VidualPayCryptoUtil(
                    creds.getClientId(),   // encryptdecryptKey → AES key
                    creds.getSaltKey()     // saltKey           → GCM IV
            );

            // ── 3. Call VidualPay authorize endpoint ───────────────────────
            String url = creds.getBaseUrlUat() + "/payoutapi/api/signature/authorizeuat";
            log.info("VidualPay authorize → POST {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("secretKey",         creds.getClientSecret());  // secretKey
            headers.set("saltKey",           creds.getSaltKey());        // saltKey
            headers.set("encryptdecryptKey", creds.getClientId());       // encryptdecryptKey
            headers.set("userId",            creds.getUsername());        // userId

            HttpEntity<Void> httpRequest = new HttpEntity<>(headers);

            ResponseEntity<VidualPayResponse> httpResponse = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    httpRequest,
                    VidualPayResponse.class
            );

            VidualPayResponse body = httpResponse.getBody();

            if (body == null) {
                throw new RuntimeException("Empty response from VidualPay authorize");
            }

            log.info("VidualPay authorize — successStatus={} responseCode={}",
                    body.getSuccessStatus(), body.getResponseCode());

            if (!Boolean.TRUE.equals(body.getSuccessStatus())) {
                throw new RuntimeException(
                        "VidualPay authorization failed: " + body.getMessage());
            }

            // ── 4. Decrypt the response data ───────────────────────────────
            String decrypted = crypto.decrypt(body.getData());
            log.info("VidualPay authorize — decrypted token received successfully");

            AuthorizationResponse result = new AuthorizationResponse(
                    body.getSuccessStatus(),
                    body.getMessage(),
                    body.getResponseCode(),
                    decrypted
            );

            response = "successStatus=" + body.getSuccessStatus()
                    + " responseCode=" + body.getResponseCode();
            responseDateTime = LocalDateTime.now();

            return result;

        } catch (Exception ex) {
            response = "Error: " + ex.getMessage();
            responseDateTime = LocalDateTime.now();
            log.error("VidualPay authorize failed: {}", ex.getMessage(), ex);
            throw ex;
        } finally {
            commonFunctions.saveImLogs(request, requestDateTime, null, response, responseDateTime);
        }
    }
}
