package com.scs.admin.util;

import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES/GCM/NoPadding crypto utility for VidualPay.
 *
 * Key mapping (as confirmed by VidualPay support):
 *   edKey  = encryptdecryptKey  (used as AES secret key)
 *   ivKey  = saltKey            (used as GCM IV / nonce)
 *
 * Sandbox credentials:
 *   encryptdecryptKey = 47cdba1911f078afe863774a8dffcb26
 *   saltKey           = bb13602c407b1ddd6506d59e410bafeb
 *
 * @author SHUBHAM KHOPADE
 */
@Slf4j
public class VidualPayCryptoUtil {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    TAG_BITS   = 128;

    private final byte[] keyBytes;
    private final byte[] ivBytes;

    /**
     * @param encryptdecryptKey  the edKey — used as AES secret key
     * @param saltKey            the ivKey — used as GCM nonce
     */
    public VidualPayCryptoUtil(String encryptdecryptKey, String saltKey) {

        this.keyBytes = encryptdecryptKey.getBytes(StandardCharsets.UTF_8);
        this.ivBytes  = saltKey.getBytes(StandardCharsets.UTF_8);

        if (keyBytes.length != 16 && keyBytes.length != 24 && keyBytes.length != 32) {
            throw new IllegalArgumentException(
                "encryptdecryptKey must produce 16, 24, or 32 bytes. Got: " + keyBytes.length);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ENCRYPT
    // ─────────────────────────────────────────────────────────────────────────

    public String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new GCMParameterSpec(TAG_BITS, ivBytes)
            );
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception ex) {
            log.error("VidualPay encrypt failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("VidualPay encrypt failed: " + ex.getMessage(), ex);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DECRYPT  (instance method — uses the same keys as encrypt)
    // ─────────────────────────────────────────────────────────────────────────

    public String decrypt(String encryptedBase64) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(
                Cipher.DECRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new GCMParameterSpec(TAG_BITS, ivBytes)
            );
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedBase64));
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception ex) {
            log.error("VidualPay decrypt failed: {}", ex.getMessage(), ex);
            throw new RuntimeException("VidualPay decrypt failed: " + ex.getMessage(), ex);
        }
    }
}
