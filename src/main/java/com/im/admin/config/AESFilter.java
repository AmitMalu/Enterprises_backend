package com.im.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.response.CommonResponse;
import com.im.admin.util.AESEncryptionUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.spec.IvParameterSpec;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

@Component
@Slf4j
public class AESFilter extends OncePerRequestFilter {

	private static final Set<String> PATHS_VIEWABLE_WITHOUT_AUTHENTICATION = Set.of("/images/","/webjars/**", "cache",
			"/api/aes/generate-token", "/auth/login", "/auth/verify-otp", "/auth/resend-otp", "/auth/set-password","/auth/forgot-password",
			"/api/aes/validate-token", "/api/pg/generate-token", "/api/pg/payment/callback","/auth/forget-verify-otp", "/api/payout/callback",
			"/api/aes/encrypt-credentials", "/api/aes/decrypt-credentials");

	@Value("${app.security.base-secret}")
	private String baseSecret;

	@Value("${app.security.pbkdf2-iterations}")
	private int iterations;

	@Value("${app.security.pbkdf2-keylength}")
	private int keyLength;

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

	@Autowired
	private AESKeyService aesKeyService;

	@Autowired
	private AESEncryptionUtil aesEncryptionUtil;

	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		String path = request.getRequestURI();
		log.info("Request URI: {}", path);
		if (this.isPathViewableWithoutAuthentication(request)) {
			log.info("User is authorized to access this resource");
			filterChain.doFilter(request, response);
		} else {
			String token = request.getHeader("Authorization");
			if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
				token = token.substring(7);

				try {
					log.info("Decrypting token...");
					String decryptedCreds = this.decryptToken(token);
					String[] creds = decryptedCreds.split("/");
					String tokenGenerationTimestampStr = creds[0];
					String expirationTimestampStr = creds[1];
					if (this.isTokenExpired(tokenGenerationTimestampStr, expirationTimestampStr)) {
						log.error("Unauthorized access - Token expired");
						this.sendErrorResponse(response, 401, "Token is expired.");
						return;
					}

					log.info("User is authorized to access this resource");
					filterChain.doFilter(request, response);
				} catch (Exception e) {
					log.error("Error processing token: {}", e.getMessage());
					this.throwException(response);
				}

			} else {
				log.warn("Request is not in whitelist and no token provided.");
				this.throwException(response);
			}
		}
	}

	private boolean isPathViewableWithoutAuthentication(HttpServletRequest request) {
		String contextPath = request.getContextPath();  // e.g., /enterprises
		log.info("<-------- context Path ------> {}", contextPath);

		String requestURI = request.getRequestURI();
		log.info("<-------- request URI ------> {}", requestURI);  // e.g., /enterprises/api/pg/payment/callback

		String path = requestURI.substring(contextPath.length());  // → /api/pg/payment/callback
		log.info("<-------- Path ------> {}", path);

		return PATHS_VIEWABLE_WITHOUT_AUTHENTICATION.stream()
				.anyMatch(path::startsWith);

	}

	private String decryptToken(String token) throws Exception {
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("Missing token");
		}

		log.info("Decrypting token: {}", token);

		// hierarchyCode|saltBase64|ivBase64|encryptedToken
		String[] parts = token.split("\\|", 4);
		if (parts.length != 4) {
			throw new IllegalArgumentException("Invalid token format. Expected hierarchy|salt|iv|encryptedToken");
		}

		String hierarchyCode = parts[0];
		String saltBase64 = parts[1];
		byte[] saltBytes = Base64.getDecoder().decode(saltBase64);

		String ivBase64 = parts[2];
		String encryptedToken = parts[3];

		log.info("Generating encryption key using hierarchy type: {}", hierarchyCode);

		// derive the same key from saltBase64
		byte[] derivedKey = aesEncryptionUtil.deriveKey(baseSecret, saltBytes, iterations, keyLength);

		IvParameterSpec ivSpec = new IvParameterSpec(Base64.getDecoder().decode(ivBase64));

		// decrypt payload
		String decryptedPayload = aesEncryptionUtil.decrypt(encryptedToken, derivedKey, ivSpec);
		return decryptedPayload;
	}


	private boolean isTokenExpired(String generationTimestamp, String expirationTimestamp) {
		try {
			long expirationTime = Long.parseLong(expirationTimestamp);
			long currentTime = System.currentTimeMillis();

			log.info("Token Generation Time: {}", generationTimestamp);
			log.info("Token Expiration Time: {}", expirationTimestamp);
			log.info("Current System Time: {}", currentTime);

			if (currentTime > expirationTime) {
				log.warn("Token is expired.");
				return true;
			}
			return false;

		} catch (NumberFormatException e) {
			log.error("Invalid timestamp format: {}", e.getMessage());
			return true; // treat invalid timestamp as expired
		}
	}

	private void sendErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
		response.setStatus(status);
		response.setContentType("application/json");

		CommonResponse<Object> commonResponse = new CommonResponse<>();
		commonResponse.setStatusCode(status);
		commonResponse.setMessage(message);
		commonResponse.setData(null);

		PrintWriter writer = response.getWriter();
		writer.write(new ObjectMapper().writeValueAsString(commonResponse));
		writer.flush();
	}


	private void throwException(HttpServletResponse response) throws IOException {
		response.setStatus(401);
		response.getWriter().write("Unauthorized request");
	}
}
