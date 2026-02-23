package com.im.admin.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;

public class FingpayEncryptionUtil {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    // 1) Load Public Key from certificate string
    public static PublicKey getPublicKeyFromCertificate(String certificateStr) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        Certificate cert = cf.generateCertificate(
                new java.io.ByteArrayInputStream(certificateStr.getBytes())
        );
        return cert.getPublicKey();
    }

    // 2) SHA-256 + Base64 (for hash header)
    public static String generateSHA256Base64(String json) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashed = digest.digest(json.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(hashed);
    }

    // 3) AES-128 Key generate
    public static SecretKey generateAESKey() throws Exception {
        KeyGenerator keyGen = KeyGenerator.getInstance("AES", "BC");
        keyGen.init(128);
        return keyGen.generateKey();
    }

    // 4) Encrypt AES session key using RSA Public Key (for eskey)
    public static String encryptSessionKeyWithPublicKey(SecretKey aesKey, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedKey = cipher.doFinal(aesKey.getEncoded());
        return Base64.getEncoder().encodeToString(encryptedKey);
    }

    // 5) Encrypt Body JSON using AES key
    public static String encryptJsonWithAES(String json, SecretKey aesKey) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding", "BC");
        cipher.init(Cipher.ENCRYPT_MODE, aesKey);
        byte[] encrypted = cipher.doFinal(json.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    // 6) Build final request
    public static FingpayRequestPayload buildFinalRequest(Object requestObject, String certificateString) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(requestObject);

        // Generate Hash
        String hash = generateSHA256Base64(json);

        // Load RSA Public Key
        PublicKey publicKey = getPublicKeyFromCertificate(certificateString);

        // Generate AES Session Key
        SecretKey aesKey = generateAESKey();

        // Encrypt AES Key → eskey
        String eskey = encryptSessionKeyWithPublicKey(aesKey, publicKey);

        // Encrypt JSON → body
        String body = encryptJsonWithAES(json, aesKey);

        return new FingpayRequestPayload(hash, eskey, body);
    }

    public static class FingpayRequestPayload {
        public String hash;
        public String eskey;
        public String body;

        public FingpayRequestPayload(String hash, String eskey, String body) {
            this.hash = hash;
            this.eskey = eskey;
            this.body = body;
        }
    }


    public static FingpayRequestPayload buildFinalRequestFromRawJson(String json, String certificateString) throws Exception {

        // Generate Hash
        String hash = generateSHA256Base64(json);

        // Load RSA Public Key
        PublicKey publicKey = getPublicKeyFromCertificate(certificateString);

        // Generate AES Session Key
        SecretKey aesKey = generateAESKey();

        // Encrypt AES Key → eskey
        String eskey = encryptSessionKeyWithPublicKey(aesKey, publicKey);

        // Encrypt JSON → body
        String body = encryptJsonWithAES(json, aesKey);

        return new FingpayRequestPayload(hash, eskey, body);
    }

}
