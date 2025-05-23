package com.im.admin.config;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.controller.AESController;
import com.im.admin.response.CommonResponse;
import com.im.admin.util.AESEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AESFilter extends OncePerRequestFilter {

	private static final Set<String> PATHS_VIEWABLE_WITHOUT_AUTHENTICATION = Set.of("/webjars/**", "cache",
			"/api/aes/generate-token", "/auth/login", "/auth/verify-otp", "/auth/resend-otp", "/auth/set-password",
			"/api/aes/validate-token");

	private final UrlPathHelper pathHelper = new UrlPathHelper();

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
	private AESController aesController;

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
				this.throwException(response);
			}
		}
	}

	private boolean isPathViewableWithoutAuthentication(HttpServletRequest request) {
		String requestURI = this.pathHelper.getPathWithinApplication(request);
		log.info("Path" + requestURI);
		return PATHS_VIEWABLE_WITHOUT_AUTHENTICATION.stream()
				.anyMatch(allowedPath -> requestURI.equals(allowedPath) || requestURI.startsWith(allowedPath));
	}

	private String decryptToken(String token) throws Exception {
		if (token == null || token.isBlank()) {
			throw new IllegalArgumentException("Missing argument: token is empty");
		}

		log.info("Decrypting token: {}", token);

		String[] parts = token.split("\\|", 2); // Split into hierarchyCode and token
		if (parts.length != 2) {
			throw new IllegalArgumentException("Invalid token format. Expected hierarchyCode|encryptedToken");
		}

		String hierarchyCode = parts[0];
		String encryptedToken = parts[1];

		String salt = HIERARCHY_SALTS.getOrDefault(hierarchyCode, "defaultSalt");

		log.info("Generating encryption key using hierarchy type: {}", hierarchyCode);

		byte[] derivedKey = AESEncryptionUtil.deriveKey(baseSecret, salt, iterations, keyLength);

		byte[] secretKey = this.aesController.getSecretKey();
		if (secretKey == null || secretKey.length == 0) {
			throw new IllegalStateException("Secret key is not initialized");
		}

		return AESEncryptionUtil.decryptEnterprises(encryptedToken, derivedKey);
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
