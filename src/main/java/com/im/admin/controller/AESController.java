package com.im.admin.controller;


import com.im.admin.config.EncryptionService;
import com.im.admin.dto.EncryptDecryptCredentialsDto;
import com.im.admin.entities.UserDetails;
import com.im.admin.repositories.UserDetailsRepository;

import com.im.admin.request.TokenGenerationRequest;
import com.im.admin.util.AESEncryptionUtil;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
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

	@Autowired
	private PasswordEncoder passwordEncoder;

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

	@Autowired
	private EncryptionService encryptionService;

	@Autowired
	private AESEncryptionUtil aesEncryptionUtil;

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
	public ResponseEntity<?> generateToken(@RequestBody TokenGenerationRequest tokenGenerationRequest) {
		try {
			// 1 Generate salt & IV
			String saltBase64 = tokenGenerationRequest.getSalt(); // client sends or you generate
			byte[] userPassIvBytes = Base64.getDecoder().decode(tokenGenerationRequest.getIvBase64());
			IvParameterSpec userPassIv = new IvParameterSpec(userPassIvBytes);

			// 2 Derive key
			byte[] tokenDerivedKey = aesEncryptionUtil.deriveKey(baseSecret, saltBase64, iterations, keyLength);

			// 3 Decrypt username/password
			String decryptedUsername = aesEncryptionUtil.decrypt(
					tokenGenerationRequest.getUsername(), tokenDerivedKey, userPassIv);
			String decryptedPassword = aesEncryptionUtil.decrypt(
					tokenGenerationRequest.getPassword(), tokenDerivedKey, userPassIv);

			// 4 Validate user/password (your existing code)
			Optional<UserDetails> optionalUser = userDetailsRepository.findByMobileNumber(decryptedUsername);
			if (optionalUser.isEmpty()) {
				optionalUser = userDetailsRepository.findByEmailId(decryptedUsername);
			}
			if (optionalUser.isEmpty()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("error", "Invalid credentials"));
			}
			UserDetails user = optionalUser.get();
			if (!passwordEncoder.matches(decryptedPassword, user.getLoginPassword())) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
						.body(Map.of("error", "Invalid credentials"));
			}

			// 5 Build payload
			long currentTime = System.currentTimeMillis();
			long expirationTime = currentTime + 3600_000L;
			String payload = currentTime + "/" + expirationTime + "/" + user.getMobileNumber();

			// 6 Encrypt payload
			IvParameterSpec tokenIv = aesEncryptionUtil.generateIv();
			String encryptedToken = aesEncryptionUtil.encrypt(payload, tokenDerivedKey, tokenIv);

			// 7 Combine everything you’ll need later to decrypt:
			// hierarchyCode|saltBase64|ivBase64|encryptedToken
			String fullToken = user.getHierarchyCode() + "|" +
					saltBase64 + "|" +
					Base64.getEncoder().encodeToString(tokenIv.getIV()) + "|" +
					encryptedToken;

			return ResponseEntity.ok(Map.of(
					"token", fullToken,
					"expiresAt", expirationTime
			));
		} catch (Exception e) {
			log.error("Error generating token", e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Error generating token: " + e.getMessage()));
		}
	}


//	@PostMapping("/validate-token")
//	public ResponseEntity<?> validateToken(@RequestBody VerifyTokenRequestDto verifyTokenRequestDto) {
//
//		String hierarchyType = verifyTokenRequestDto.getHierarchyType();
//		String token = verifyTokenRequestDto.getToken();
//
//		try {
//			String salt = HIERARCHY_SALTS.getOrDefault(hierarchyType, "defaultSalt");
//			byte[] derivedKey = AESEncryptionUtil.deriveKey(baseSecret, salt, iterations, keyLength);
//
//			log.info("Received token: {}", token);
//			String decryptedPayload = AESEncryptionUtil.decryptEnterprises(token, derivedKey);
//			String[] parts = decryptedPayload.split("/");
//
//			long issuedAt = Long.parseLong(parts[0]);
//			long expiresAt = Long.parseLong(parts[1]);
//			String username = parts[2];
//
//			if (System.currentTimeMillis() > expiresAt) {
//				return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//						.body("Token has expired.");
//			}
//
//			return ResponseEntity.ok(Map.of(
//					"username", username,
//					"issuedAt", issuedAt,
//					"expiresAt", expiresAt
//			));
//		} catch (Exception e) {
//			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
//					.body("Invalid token: " + e.getMessage());
//		}
//	}

	@PostMapping("/encrypt-credentials")
	public ResponseEntity<?> encryptCredentials(@RequestBody EncryptDecryptCredentialsDto encryptDecryptCredentialsDto ) {
		try {
			// Generate one salt & IV for the request
			String salt = aesEncryptionUtil.generateRandomSalt();
			IvParameterSpec iv = aesEncryptionUtil.generateIv();
			String ivBase64 = Base64.getEncoder().encodeToString(iv.getIV());

			byte[] derivedKey = aesEncryptionUtil.deriveKey(baseSecret, salt, iterations, keyLength);

			String encryptedUsername = aesEncryptionUtil.encrypt(encryptDecryptCredentialsDto.getUsername(), derivedKey, iv );
			String encryptedPassword = aesEncryptionUtil.encrypt(encryptDecryptCredentialsDto.getPassword(), derivedKey, iv );

			return ResponseEntity.ok(Map.of(
					"cipherUsername", encryptedUsername,
					"cipherPassword", encryptedPassword,
					"salt", salt,
					"iv", ivBase64
			));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", e.getMessage()));
		}
	}

	@PostMapping("/decrypt-credentials")
	public ResponseEntity<?> decryptCredentials(@RequestBody EncryptDecryptCredentialsDto decryptCredentials) {
		try {
			// 1️ IV decode
			byte[] ivBytes = Base64.getDecoder().decode(decryptCredentials.getIv());
			IvParameterSpec ivSpec = new IvParameterSpec(ivBytes);

			// 3️ Derive AES key using decoded salt
			byte[] secretKey = aesEncryptionUtil.deriveKey(
					baseSecret,
					decryptCredentials.getSalt(),
					iterations,
					keyLength
			);

			// 4️ Decrypt username & password
			String decryptedUsername = aesEncryptionUtil.decrypt(decryptCredentials.getUsername(), secretKey, ivSpec);
			String decryptedPassword = aesEncryptionUtil.decrypt(decryptCredentials.getPassword(), secretKey, ivSpec);

			return ResponseEntity.ok(Map.of(
					"username", decryptedUsername,
					"password", decryptedPassword
			));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", e.getMessage()));
		}
	}

}
