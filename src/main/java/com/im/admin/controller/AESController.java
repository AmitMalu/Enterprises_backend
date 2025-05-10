package com.im.admin.controller;


import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.UserDetailsRepository;

import com.im.admin.util.AESEncryptionUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.log4j.Log4j2;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.*;

@RestController
@Log4j2
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

	private byte[] SECRET_KEY;

	@Autowired
	private UserDetailsRepository userDetailsRepository;

	public byte[] getSecretKey() {
		return this.SECRET_KEY;
	}


	@PostMapping("/generate-token")
	public ResponseEntity<?> generateToken(String username) {
		try {
			Optional<UserDetails> optionalUser = userDetailsRepository.findByMobileNumber(username);
			if (optionalUser.isEmpty()) {
				optionalUser = userDetailsRepository.findByEmail(username);
			}

			if (optionalUser.isEmpty()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body("Invalid credentials: user not found");
			}

			UserDetails user = optionalUser.get();
			String hierarchyType = user.getHierarchyCode();
			String salt = HIERARCHY_SALTS.getOrDefault(hierarchyType, "defaultSalt");

			byte[] derivedKey = AESEncryptionUtil.deriveKey(baseSecret, salt, iterations, keyLength);

			long currentTime = System.currentTimeMillis();
			long expirationTime = currentTime + 3600000L; // 1 hour

			String payload = currentTime + "/" + expirationTime + "/" + username;
			IvParameterSpec iv = AESEncryptionUtil.generateIv();
			String encryptedToken = AESEncryptionUtil.encryptEnterprises(payload, derivedKey, iv);

			return ResponseEntity.ok(Map.of(
					"token", encryptedToken,
					"expiresIn", expirationTime
			));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body("Error generating token: " + e.getMessage());
		}
	}

	@PostMapping("/validate-token")
	public ResponseEntity<?> validateToken(@RequestParam String token, @RequestParam String hierarchyType) {
		try {
			String salt = HIERARCHY_SALTS.getOrDefault(hierarchyType, "defaultSalt");
			byte[] derivedKey = AESEncryptionUtil.deriveKey(baseSecret, salt, iterations, keyLength);

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

	// Helper method to generate AES secret key using PBKDF2
	private byte[] generateSecretKey(int partnerId) throws Exception {
		String encryptedSecretKey = SALT_KEY + partnerId;
		byte[] salt = SALT_KEY.getBytes(StandardCharsets.UTF_8);
		int iterationCount = 10000;
		int keyLength = 256; // AES-256 requires a 256-bit key (32 bytes)

		// Generate the AES key using PBKDF2
		KeySpec spec = new PBEKeySpec(encryptedSecretKey.toCharArray(), salt, iterationCount, keyLength);
		SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		return factory.generateSecret(spec).getEncoded();
	}

//	@PostMapping("/encrypt")
//	public ResponseEntity<String> encryptPassword(@RequestBody UserCredentials userCredentials) {
//		try {
//			IvParameterSpec ivParameterSpec = AESEncryptionUtil.generateIv();
//			String encryptedPassword = AESEncryptionUtil.encrypt(
//					userCredentials.getUsername() + "/" + userCredentials.getPassword(), SECRET_KEY, ivParameterSpec);
//			return ResponseEntity.ok("encryptedPassword: " + encryptedPassword);
//		} catch (Exception e) {
//			return ResponseEntity.status(500).body("Error encrypting password: " + e.getMessage());
//		}
//	}
//
//	@PostMapping("/decrypt")
//	public ResponseEntity<String> decryptPassword(@RequestBody EncryptedPassword encryptedPassword) {
//		try {
//			String decryptedPassword = AESEncryptionUtil.decrypt(encryptedPassword.getEncryptedPassword(), SECRET_KEY);
//			return ResponseEntity.ok("Decrypted Password: " + decryptedPassword);
//		} catch (Exception e) {
//			log.error("Decryption error: {}", e.getMessage());
//			return ResponseEntity.status(500).body("Error decrypting password: " + e.getMessage());
//		}
//
//	}
}
