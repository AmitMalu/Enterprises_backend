package com.im.admin.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AESEncryptionUtil {

    @Generated
    private static final Logger log = LoggerFactory.getLogger(AESEncryptionUtil.class);
    private static final String AES = "AES";
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";
    private static final int IV_SIZE = 16;

    public static SecretKey generateKey(byte[] secretKey) {
        return new SecretKeySpec(secretKey, AES);
    }

    public static IvParameterSpec generateIv() {
        byte[] iv = new byte[IV_SIZE];
        (new SecureRandom()).nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    // ======= New PBKDF2 key derivation =======
    public static byte[] deriveKey(String baseSecret, String salt, int iterations, int keyLength) throws Exception {
        log.info("Starting key derivation with salt: {}", salt);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(baseSecret.toCharArray(), salt.getBytes(), iterations, keyLength);
        byte[] key = factory.generateSecret(spec).getEncoded();

        log.info("Derived key successfully: {}", Arrays.toString(key));
        return key;
    }

    public static String encryptEnterprises(String data, byte[] secretKey, IvParameterSpec iv) throws Exception {
        log.info("Secret key during encryption: {}", Arrays.toString(secretKey));
        SecretKey key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        byte[] ivAndCipherText = concatenate(iv.getIV(), encryptedBytes);

        String base64Encoded = Base64.getEncoder().encodeToString(ivAndCipherText);
        log.info("Encryption successful. Encrypted data (Base64): {}", base64Encoded);
        return base64Encoded;
    }

    public static String decryptEnterprises(String base64CipherText, byte[] secretKey) throws Exception {
        log.info("Starting decryption for Base64 token.");

        base64CipherText = base64CipherText.replaceAll("\\s+", "");
        int missingPadding = (4 - (base64CipherText.length() % 4)) % 4;
        base64CipherText += "=".repeat(missingPadding);

        byte[] ivAndCipherText;
        try {
            ivAndCipherText = Base64.getDecoder().decode(base64CipherText);
        } catch (IllegalArgumentException e) {
            log.error("Token is not valid Base64: {}", e.getMessage());
            throw new IllegalArgumentException("Token is not valid Base64: " + e.getMessage(), e);
        }

        if (ivAndCipherText.length < IV_SIZE) {
            log.warn("Cipher text too short to contain IV.");
            throw new IllegalArgumentException("Cipher text too short to contain IV");
        }

        byte[] ivBytes = Arrays.copyOfRange(ivAndCipherText, 0, IV_SIZE);
        byte[] cipherBytes = Arrays.copyOfRange(ivAndCipherText, IV_SIZE, ivAndCipherText.length);

        SecretKey key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivBytes));

        byte[] decryptedBytes = cipher.doFinal(cipherBytes);
        String decryptedData = new String(decryptedBytes, StandardCharsets.UTF_8);

        log.info("Decryption successful. Decrypted data: {}", decryptedData);
        return decryptedData;

    }

    private static byte[] concatenate(byte[] iv, byte[] cipherText) {
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
        return combined;
    }

}
