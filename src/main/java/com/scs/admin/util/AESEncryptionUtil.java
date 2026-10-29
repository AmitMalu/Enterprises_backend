package com.scs.admin.util;

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

import com.scs.admin.dto.EncryptedData;
import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AESEncryptionUtil {

    @Generated
    private static final String AES = "AES";
    private static final String AES_CBC = "AES/CBC/PKCS5Padding";
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int IV_SIZE = 16;
    private static final int SALT_SIZE = 16; // 16 bytes = 128 bits

    public SecretKey generateKey(byte[] secretKey) {
        return new SecretKeySpec(secretKey, AES);
    }

    public String generateRandomSalt() {
        byte[] salt = new byte[SALT_SIZE];
        secureRandom.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public IvParameterSpec generateIv() {
        byte[] iv = new byte[IV_SIZE];
        (new SecureRandom()).nextBytes(iv);
        return new IvParameterSpec(iv);
    }

    // ======= New PBKDF2 key derivation =======
    public byte[] deriveKey(String baseSecret, byte[] salt, int iterations, int keyLength) throws Exception {
        log.info("Starting key derivation with salt: {}", salt);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(baseSecret.toCharArray(), salt, iterations, keyLength);
        byte[] key = factory.generateSecret(spec).getEncoded();

        log.info("Derived key successfully: {}", Arrays.toString(key));
        return key;
    }

    public String encrypt(String plaintext, byte[] derivedKey, IvParameterSpec iv) throws Exception {
        SecretKey key = generateKey(derivedKey);
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.ENCRYPT_MODE, key, iv);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }


    public String decrypt(String cipherTextBase64, byte[] derivedKey, IvParameterSpec iv) throws Exception {
        SecretKey key = generateKey(derivedKey);
        Cipher cipher = Cipher.getInstance(AES_CBC);
        cipher.init(Cipher.DECRYPT_MODE, key, iv);
        byte[] decoded = Base64.getDecoder().decode(cipherTextBase64);
        byte[] decryptedBytes = cipher.doFinal(decoded);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

}
