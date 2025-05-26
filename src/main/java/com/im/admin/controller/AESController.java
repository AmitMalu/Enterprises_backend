package com.im.admin.controller;


import com.im.admin.dto.VerifyTokenRequestDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.UserDetailsRepository;

import com.im.admin.util.AESEncryptionUtil;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.*;

@RestController
@Slf4j
@RequestMapping("/api/aes")
public class AESController {

	@Value("${app.security.base-secret}")
	private String baseSecret;

	@Value("${app.security.pbkdf2-iterations}")
	private int iterations;

	@Value("${app.security.pbkdf2-keylength}")
	private int keyLength;

	private static final String SALT_KEY = "j8cLQbZTrMDQnNhu";

	private static final Map<String, String> HIERARCHY_SALTS = Map.of(
			"RT", "nP4jXs9mQwU2dT8v",        // Retailer
			"DT", "fG9kRz1bYpV8jQwT",        // Distributor
			"PT", "hD1fL9zWjT4gM7pR",        // Partner
			"SD", "sZ7dEx2kLwV3tQrM",        // SuperDistributor
			"WL", "kL7tF1yXsQp3vJbW",        // Whitelabel
			"ET", "dP5mSzR2xTgWq4Bv",        // Enterprise
			"AD", "j5vR6fTpCq9LzH2b",        // Admin
			"SA", "xC8mV2r9LpW6qTjZ"         // SuperAdmin
	);

	private byte[] defaultSecretKey;

	@Autowired
	private UserDetailsRepository userDetailsRepository;

	@PostConstruct
	public void init() {
		// Initialize the default key using the fallback SALT_KEY
		this.defaultSecretKey = generateSecretKey(baseSecret, SALT_KEY, iterations, keyLength);
		log.info("Default AES secret key initialized.");
	}

	/**
	 * Returns the default AES secret key.
	 */
	public byte[] getSecretKey() {
		if (this.defaultSecretKey == null) {
			throw new IllegalStateException("Secret key is not initialized.");
		}
		log.info("SecretKey --------->" + defaultSecretKey);
		return this.defaultSecretKey;
	}

	/**
	 * Returns a specific AES secret key based on hierarchy type (e.g., RT, DT, PT).
	 */
	public byte[] getSecretKeyForHierarchy(String hierarchyCode) {
		String salt = HIERARCHY_SALTS.getOrDefault(hierarchyCode, SALT_KEY);
		return generateSecretKey(baseSecret, salt, iterations, keyLength);
	}

	/**
	 * Generates a secret key using PBKDF2.
	 */
	private byte[] generateSecretKey(String baseSecret, String salt, int iterations, int keyLength) {
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
			KeySpec spec = new PBEKeySpec(baseSecret.toCharArray(), salt.getBytes(StandardCharsets.UTF_8), iterations, keyLength);
			return factory.generateSecret(spec).getEncoded();
		} catch (Exception e) {
			log.error("Error generating secret key: {}", e.getMessage(), e);
			throw new RuntimeException("Failed to generate secret key", e);
		}
	}

	@PostMapping("/generate-token")
	public ResponseEntity<?> generateToken(String username) {
		try {
			log.info("Attempting to generate token for username: {}", username);

			Optional<UserDetails> optionalUser = userDetailsRepository.findByMobileNumber(username);
			if (optionalUser.isEmpty()) {
				optionalUser = userDetailsRepository.findByEmailId(username);
			}

			if (optionalUser.isEmpty()) {
				log.warn("User not found for username: {}", username);
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("Invalid credentials: user not found");
			}

			UserDetails user = optionalUser.get();
			String hierarchyType = user.getHierarchyCode();
			String salt = HIERARCHY_SALTS.getOrDefault(hierarchyType, "defaultSalt");

			log.info("Generating encryption key using hierarchy type: {}", hierarchyType);

			byte[] derivedKey = AESEncryptionUtil.deriveKey(baseSecret, salt, iterations, keyLength);

			long currentTime = System.currentTimeMillis();
			long expirationTime = currentTime + 3600000L; // 1 hour

			String payload = currentTime + "/" + expirationTime + "/" + username;
			IvParameterSpec iv = AESEncryptionUtil.generateIv();
			String encryptedToken = AESEncryptionUtil.encryptEnterprises(payload, derivedKey, iv);

			log.info("Token generated successfully for username: {}", username);

			String token = hierarchyType + "|" + encryptedToken;

			return ResponseEntity.ok(Map.of(
					"token", token
			));
		} catch (Exception e) {
			log.error("Error generating token for username: {}", username, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error generating token: " + e.getMessage());
		}
	}

	@PostMapping("/validate-token")
	public ResponseEntity<?> validateToken(@RequestBody VerifyTokenRequestDto verifyTokenRequestDto) {

		String hierarchyType = verifyTokenRequestDto.getHierarchyType();
		String token = verifyTokenRequestDto.getToken();

		try {
			String salt = HIERARCHY_SALTS.getOrDefault(hierarchyType, "defaultSalt");
			byte[] derivedKey = AESEncryptionUtil.deriveKey(baseSecret, salt, iterations, keyLength);

			log.info("Received token: {}", token);
			String decryptedPayload = AESEncryptionUtil.decryptEnterprises(token, derivedKey);
			String[] parts = decryptedPayload.split("/");

			long issuedAt = Long.parseLong(parts[0]);
			long expiresAt = Long.parseLong(parts[1]);
			String username = parts[2];

			if (System.currentTimeMillis() > expiresAt) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("Token has expired.");
			}

			return ResponseEntity.ok(Map.of(
					"username", username,
					"issuedAt", issuedAt,
					"expiresAt", expiresAt
			));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body("Invalid token: " + e.getMessage());
		}
	}

}
