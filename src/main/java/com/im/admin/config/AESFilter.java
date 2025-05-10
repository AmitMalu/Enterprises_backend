package com.im.admin.config;

import java.io.IOException;
import java.util.Set;

import com.im.admin.controller.AESController;
import com.im.admin.util.AESEncryptionUtil;
import org.springframework.beans.factory.annotation.Autowired;
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
			if (!StringUtils.hasText(token) && token.length() > 7) {
				token = token.substring(7);

				try {
					log.info("Decrypting token...");
					String decryptedCreds = this.decryptToken(token);
					String[] creds = decryptedCreds.split("/");
					String tokenGenerationTimestampStr = creds[0];
					String expirationTimestampStr = creds[1];
					if (this.isTokenExpired(tokenGenerationTimestampStr, expirationTimestampStr)) {
						log.error("Unauthorized access - Token expired");
						this.throwException(response);
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
		byte[] secretKey = this.aesController.getSecretKey();
		return AESEncryptionUtil.decryptss(token, secretKey);
	}
	
	// Fix the expiration check in the filter
	private boolean isTokenExpired(String generationTimestamp, String expirationTimestamp) {
	    try {
	        long expirationTime = Long.parseLong(expirationTimestamp);
	        long currentTime = System.currentTimeMillis(); // Get current time

	        log.info("Token Generation Time: {}", generationTimestamp);
	        log.info("Token Expiration Time: {}", expirationTimestamp);
	        log.info("Current System Time: {}", currentTime);

	        boolean isExpired = currentTime > expirationTime;
	        if (isExpired) {
	            log.warn("Token is expired. Current time [{}] is greater than expiration time [{}]", currentTime, expirationTime);
	            throw new RuntimeException("Token is expired...");
	        } else {
	            log.info("Token is still valid.");
	        }

	        return isExpired;
	    } catch (NumberFormatException e) {
	        log.error("Invalid timestamp format. Generation: [{}], Expiration: [{}], Error: {}", generationTimestamp, expirationTimestamp, e.getMessage());
	        return true; // Assume invalid token if parsing fails
	    }
	}

	private void throwException(HttpServletResponse response) throws IOException {
		response.setStatus(401);
		response.getWriter().write("Unauthorized request");
	}
}
