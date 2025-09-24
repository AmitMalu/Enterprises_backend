package com.im.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Map;

@Service
public class AESKeyService {

    @Value("${app.security.base-secret}")
    private String baseSecret;

    @Value("${app.security.pbkdf2-iterations}")
    private int iterations;

    @Value("${app.security.pbkdf2-keylength}")
    private int keyLength;

    private static final String DEFAULT_SALT = "j8cLQbZTrMDQnNhu";

    private static final Map<String, String> HIERARCHY_SALTS = Map.of(
            "RT", "nP4jXs9mQwU2dT8v",
            "DT", "fG9kRz1bYpV8jQwT",
            "PT", "hD1fL9zWjT4gM7pR",
            "SD", "sZ7dEx2kLwV3tQrM",
            "WL", "kL7tF1yXsQp3vJbW",
            "ET", "dP5mSzR2xTgWq4Bv",
            "AD", "j5vR6fTpCq9LzH2b",
            "SA", "xC8mV2r9LpW6qTjZ"
    );

    public byte[] getSecretKey() {
        return generateSecretKey(baseSecret, DEFAULT_SALT, iterations, keyLength);
    }

    public byte[] getSecretKeyForHierarchy(String hierarchyCode) {
        String salt = HIERARCHY_SALTS.getOrDefault(hierarchyCode, DEFAULT_SALT);
        return generateSecretKey(baseSecret, salt, iterations, keyLength);
    }

    private byte[] generateSecretKey(String baseSecret, String salt, int iterations, int keyLength) {
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            KeySpec spec = new PBEKeySpec(baseSecret.toCharArray(),
                    salt.getBytes(StandardCharsets.UTF_8),
                    iterations, keyLength);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate secret key", e);
        }
    }
}
