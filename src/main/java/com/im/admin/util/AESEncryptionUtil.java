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
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(baseSecret.toCharArray(), salt.getBytes(), iterations, keyLength);
        return factory.generateSecret(spec).getEncoded();
    }

    public static String encryptEnterprises(String data, byte[] secretKey, IvParameterSpec iv) throws Exception {
        log.info("Secret key during encryption: {}", Arrays.toString(secretKey));
        SecretKey key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);

        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        byte[] ivAndCipherText = concatenate(iv.getIV(), encryptedBytes);

        String base64Encoded = Base64.getEncoder().encodeToString(ivAndCipherText);
        log.info("Encrypted data (Base64): {}", base64Encoded);
        return base64Encoded;
    }

    public static String decryptEnterprises(String base64CipherText, byte[] secretKey) throws Exception {
        byte[] ivAndCipherText = Base64.getDecoder().decode(base64CipherText);

        byte[] ivBytes = Arrays.copyOfRange(ivAndCipherText, 0, IV_SIZE);
        byte[] cipherBytes = Arrays.copyOfRange(ivAndCipherText, IV_SIZE, ivAndCipherText.length);

        SecretKey key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(ivBytes));

        byte[] decryptedBytes = cipher.doFinal(cipherBytes);
        String decryptedText = new String(decryptedBytes);
        log.info("Decrypted text: {}", decryptedText);
        return decryptedText;
    }

    private static byte[] concatenateEnterprises(byte[] a, byte[] b) {
        byte[] result = new byte[a.length + b.length];
        System.arraycopy(a, 0, result, 0, a.length);
        System.arraycopy(b, 0, result, a.length, b.length);
        return result;
    }

    public static String encrypt(String data, byte[] secretKey, IvParameterSpec iv) throws Exception {
        log.info("Secret key during encryption: {}", Arrays.toString(secretKey));
        SecretKey key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(1, key, iv);
        log.info("Extracted IV: {}", iv);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes());
        byte[] ivAndCipherText = concatenate(iv.getIV(), encryptedBytes);
        log.info("IV used during encryption: {}", Arrays.toString(iv.getIV()));
        log.info("Encrypted data: {}", Base64.getEncoder().encodeToString(ivAndCipherText));
        return Base64.getEncoder().encodeToString(ivAndCipherText);
    }

    public static String decryptss(String encryptedData, byte[] secretKey) throws Exception {
        log.info("Secret key during decryption: {}", Arrays.toString(secretKey));
        log.info("Data to be decrypted (*************): {}", encryptedData);
        byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
        byte[] iv = Arrays.copyOfRange(decodedBytes, 0, 16);
        log.info("Extracted IV: {}", Arrays.toString(iv));
        byte[] cipherText = Arrays.copyOfRange(decodedBytes, 16, decodedBytes.length);
        SecretKey key = generateKey(secretKey);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(2, key, new IvParameterSpec(iv));
        log.info("Encrypted data before decoding: {}", encryptedData);
        byte[] decryptedBytes = cipher.doFinal(cipherText);
        String decryptedData = new String(decryptedBytes, StandardCharsets.UTF_8);
        log.info("Decrypted data: {}", decryptedData);
        return decryptedData;
    }

    public static String decrypts(String encryptedData, byte[] secretKey) throws Exception {
        byte[] decodedBytess = Base64.getDecoder().decode(encryptedData);
        log.info("", decodedBytess);
        log.info("Raw token received: {}", encryptedData);
        encryptedData = sanitizeBase64(encryptedData);
        log.info("Base64 String Length after sanitization: {}", encryptedData.length());
        log.info("Sanitized and padded Base64: {}", encryptedData);

        byte[] decodedBytes;
        try {
            if (!encryptedData.contains("-") && !encryptedData.contains("_")) {
                decodedBytes = Base64.getDecoder().decode(encryptedData);
            } else {
                log.info("Detected URL-safe Base64 encoding");
                decodedBytes = Base64.getUrlDecoder().decode(encryptedData);
            }

            log.info("Decoded Base64: {}", Arrays.toString(decodedBytes));
        } catch (IllegalArgumentException e) {
            log.error("Base64 decoding failed: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid Base64 data", e);
        }

        log.info("Decoded data length: {}", decodedBytes.length);
        if (decodedBytes.length < 16) {
            log.error("Decoded data too short for IV and ciphertext.");
            throw new IllegalArgumentException("Invalid encoded data: insufficient length.");
        } else {
            byte[] iv = Arrays.copyOfRange(decodedBytes, 0, 16);
            byte[] cipherText = Arrays.copyOfRange(decodedBytes, 16, decodedBytes.length);
            if (cipherText.length % 16 != 0) {
                log.error("Invalid Ciphertext length: {}", cipherText.length);
                throw new IllegalArgumentException("Ciphertext length must be a multiple of 16 bytes.");
            } else {
                IvParameterSpec ivSpec = new IvParameterSpec(iv);
                SecretKey key = generateKey(secretKey);
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(2, key, ivSpec);

                try {
                    byte[] decryptedBytes = cipher.doFinal(cipherText);
                    String decryptedString = new String(decryptedBytes);
                    log.info("Decrypted data: {}", decryptedString);
                    return decryptedString;
                } catch (Exception e) {
                    log.error("Decryption failed: {}", e.getMessage(), e);
                    throw new IllegalArgumentException("Decryption failed", e);
                }
            }
        }
    }

    private static String sanitizeBase64(String base64) {
        log.info("Original Base64: {}", base64);
        base64 = base64.replace('-', '+').replace('_', '/');
        log.info("After URL-safe Base64 replacement: {}", base64);
        base64 = base64.replaceAll("[^A-Za-z0-9+/=]", "");
        log.info("After removing non-Base64 characters: {}", base64);
        return fixPadding(base64);
    }

    private static String fixPadding(String input) {
        int paddingRequired = 4 - input.length() % 4;
        if (paddingRequired != 4) {
            input = input + "=".repeat(paddingRequired);
        }

        return input;
    }

    public static String decrypt(String encryptedData, byte[] secretKey) throws Exception {
        log.info("Encrypted data before decoding: {}", encryptedData);
        encryptedData = sanitizeBase64(encryptedData);
        if (!encryptedData.contains("-") && !encryptedData.contains("_")) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
                log.info("Decoded data length: {}", decodedBytes.length);
                return decryptData(decodedBytes, secretKey);
            } catch (IllegalArgumentException e) {
                log.error("Standard Base64 decode failed", e);
                throw new IllegalArgumentException("Failed to decode Base64 data", e);
            }
        } else {
            log.info("Detected URL-safe Base64 encoding");

            try {
                byte[] decodedBytes = Base64.getUrlDecoder().decode(encryptedData);
                log.info("Decoded data length: {}", decodedBytes.length);
                return decryptData(decodedBytes, secretKey);
            } catch (IllegalArgumentException e) {
                log.error("Base64 decoding failed: {}", e.getMessage());
                throw new IllegalArgumentException("Invalid Base64 encoded data", e);
            }
        }
    }

    private static String decryptData(byte[] encryptedData, byte[] secretKey) throws Exception {
        log.info("Extracting IV and Ciphertext from decoded data");
        byte[] iv = Arrays.copyOfRange(encryptedData, 0, 16);
        byte[] cipherText = Arrays.copyOfRange(encryptedData, 16, encryptedData.length);
        if (cipherText.length % 16 != 0) {
            log.error("Invalid Ciphertext length: {}", cipherText.length);
            throw new IllegalArgumentException("Ciphertext length must be a multiple of 16 bytes.");
        } else {
            SecretKey key = generateKey(secretKey);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(2, key, new IvParameterSpec(iv));

            try {
                byte[] decryptedBytes = cipher.doFinal(cipherText);
                log.info("Decrypted raw bytes: {}", Arrays.toString(decryptedBytes));
                return new String(decryptedBytes, StandardCharsets.UTF_8);
            } catch (Exception e) {
                log.error("Decryption failed: {}", e.getMessage(), e);
                throw e;
            }
        }
    }

    private static byte[] concatenate(byte[] iv, byte[] cipherText) {
        byte[] combined = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);
        return combined;
    }

    private static byte[] extractIV(byte[] combined) {
        return Arrays.copyOfRange(combined, 0, 16);
    }

    private static byte[] extractCipherText(byte[] combined) {
        return Arrays.copyOfRange(combined, 16, combined.length);
    }
}
