package com.im.admin.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public class BillAvenueCryptoUtil {

    // IV from vendor PHP code
    private static final String IV_HEX = "000102030405060708090A0B0C0D0E0F";

    private static byte[] hexToBytes(String hex) {
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) (
                    (Character.digit(hex.charAt(i), 16) << 4)
                            + Character.digit(hex.charAt(i + 1), 16)
            );
        }
        return bytes;
    }

    // PHP md5($working_key)
    private static byte[] md5Key(String workingKey) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        return md.digest(workingKey.getBytes(StandardCharsets.UTF_8));
    }

    //  Encrypt → HEX
    public static String encrypt(String plainText, String workingKey) throws Exception {
        byte[] keyBytes = md5Key(workingKey);
        byte[] ivBytes = hexToBytes(IV_HEX);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(ivBytes));

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // Convert to HEX like PHP bin2hex()
        StringBuilder sb = new StringBuilder();
        for (byte b : encrypted) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    //  Decrypt from HEX
    public static String decrypt(String encHex, String workingKey) throws Exception {
        byte[] keyBytes = md5Key(workingKey);
        byte[] ivBytes = hexToBytes(IV_HEX);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE,
                new SecretKeySpec(keyBytes, "AES"),
                new IvParameterSpec(ivBytes));

        byte[] encryptedBytes = hexToBytes(encHex);
        byte[] decrypted = cipher.doFinal(encryptedBytes);

        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
