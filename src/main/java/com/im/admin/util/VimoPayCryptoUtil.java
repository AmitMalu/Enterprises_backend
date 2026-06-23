package com.im.admin.util;

import com.im.admin.entities.VendorCredentials;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * @author SHUBHAM KHOPADE
 */
@Service
public class VimoPayCryptoUtil {

    // -------------------------------
    // Encrypt using vendor's AES/GCM
    // -------------------------------
    public String encryptForVendor(VendorCredentials vendorCredentials, String plainText) {

        try {
            String secretKey = vendorCredentials.getClientId();
            String saltKey = vendorCredentials.getClientSecret();

            byte[] key = secretKey.getBytes("UTF-8");
            byte[] iv = saltKey.getBytes("UTF-8");

            if (key.length != 16 && key.length != 24 && key.length != 32) {
                throw new IllegalArgumentException("AES Key must be 16, 24 or 32 bytes");
            }

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);

        } catch (Exception e) {
            throw new RuntimeException("Error during vendor AES encryption", e);
        }
    }


    // -------------------------------
    // Decrypt vendor response AES/GCM
    // -------------------------------
    public String decryptFromVendor(VendorCredentials vendorCredentials, String ciphertext) {

        if (ciphertext == null || ciphertext.isBlank()) {
            throw new IllegalArgumentException("Encrypted vendor response is null or empty");
        }

        try {
            String secretKey = vendorCredentials.getClientSecret();
            String saltKey = vendorCredentials.getSaltKey();

            byte[] key = secretKey.getBytes("UTF-8");
            byte[] iv = saltKey.getBytes("UTF-8");

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] decoded = Base64.getDecoder().decode(ciphertext);
            byte[] decrypted = cipher.doFinal(decoded);

            return new String(decrypted, "UTF-8");

        } catch (Exception e) {
            throw new RuntimeException("Error during vendor AES decryption", e);
        }
    }
}
