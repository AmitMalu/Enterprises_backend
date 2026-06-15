package com.im.admin.service.impl;

import com.im.admin.entities.VendorCredentials;
import com.im.admin.response.AuthorizationResponse;
import com.im.admin.response.VidualPayResponse;
import com.im.admin.util.VidualPayCryptoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * @author SHUBHAM KHOPADE
 */

@Service
public class VidualPayServiceImpl {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private VidualPayCryptoUtil vidualPayCryptoUtil;


    public AuthorizationResponse authorize(VendorCredentials vendorCreds) {

        String url =
                vendorCreds.getBaseUrlUat()
                        + "/payoutapi/api/signature/authorizeuat";

        HttpHeaders headers =
                new HttpHeaders();

        headers.set(
                "secretKey",
                vendorCreds.getClientSecret()
        );

        headers.set(
                "saltKey",
                vendorCreds.getSaltKey()
        );

        headers.set(
                "encryptdecryptKey",
                vendorCreds.getClientId()
        );

        headers.set(
                "userId",
                vendorCreds.getUsername()
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        ResponseEntity<VidualPayResponse> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.POST,
                        request,
                        VidualPayResponse.class
                );

        VidualPayResponse body =
                response.getBody();

        if (body == null) {
            throw new RuntimeException(
                    "Empty response"
            );
        }

        String decrypted =
                VidualPayCryptoUtil.decrypt(
                        body.getData()
                );

        return new AuthorizationResponse(
                body.getSuccessStatus(),
                body.getMessage(),
                body.getResponseCode(),
                decrypted
        );
    }
}
