package com.im.admin.controller;

import com.im.admin.request.EncryptedRequest;
import com.im.admin.request.VidualPayAuthorizeRequest;
import com.im.admin.response.AuthorizationResponse;
import com.im.admin.response.CommonResponse;
import com.im.admin.service.VidualPayService;
import com.im.admin.util.VidualPayCryptoUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * VidualPay payout controller.
 *
 * POST /api/vidual-pay/authorize    → Partner authorization
 * POST /api/vidual-pay/encrypt      → Utility: encrypt a payload
 * POST /api/vidual-pay/decrypt      → Utility: decrypt a payload
 *
 * @author SHUBHAM KHOPADE
 */
@RestController
@Slf4j
@RequestMapping("/api/vidual-pay")
@RequiredArgsConstructor
public class VidualPayoutController {

    private final VidualPayService vidualPayService;
    private final VidualPayCryptoUtil vidualPayCryptoUtil;

    // ─────────────────────────────────────────────────────────────────────────
    // AUTHORIZE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Partner Authorization API.
     *
     * Internally calls POST /payoutapi/api/signature/authorizeuat on VidualPay
     * with the four mandatory headers (secretKey, saltKey, encryptdecryptKey, userId)
     * read from the vendor_credentials table identified by vendorCredentialsId.
     *
     * The encrypted token in the response is decrypted before returning.
     *
     * Request body:
     * {
     *   "vendorCredentialsId": 1
     * }
     */
    @PostMapping("/authorize")
    public ResponseEntity<CommonResponse<AuthorizationResponse>> authorize(
            @Valid @RequestBody VidualPayAuthorizeRequest request) {

        log.info("VidualPay authorize — vendorCredentialsId={}", request.getVendorCredentialsId());

        AuthorizationResponse result = vidualPayService.authorize(request.getVendorCredentialsId());

        return ResponseEntity.status(HttpStatus.OK).body(
                CommonResponse.success(result, "VidualPay authorization successful")
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UTILITY — encrypt / decrypt
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Utility endpoint — encrypts a plain-text payload using the configured keys.
     * Useful for building encrypted request bodies for other VidualPay APIs.
     */
    @PostMapping("/encrypt")
    public ResponseEntity<String> encrypt(@RequestBody String plainText) {
        log.info("VidualPay encrypt utility called");
        return ResponseEntity.ok(vidualPayCryptoUtil.encrypt(plainText));
    }

    /**
     * Utility endpoint — decrypts a Base64 AES/GCM cipher text.
     */
    @PostMapping("/decrypt")
    public ResponseEntity<String> decrypt(@RequestBody EncryptedRequest request) {
        log.info("VidualPay decrypt utility called");
        return ResponseEntity.ok(vidualPayCryptoUtil.decrypt(request.getData()));
    }
}
