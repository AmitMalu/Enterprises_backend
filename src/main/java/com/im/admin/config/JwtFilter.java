package com.im.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.im.admin.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of("/auth/login", "/auth/register");

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        try {

            String requestURI = request.getServletPath();

            if (PUBLIC_PATHS.contains(requestURI)) {
                chain.doFilter(request, response);
                return;
            }

            final String authHeader = request.getHeader("Authorization");

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
                return;
            }

            String token = authHeader.substring(7);
            String username = jwtUtil.extractUsername(token);

            if (username != null && jwtUtil.validateToken(token)) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(new User(username, "", Collections.emptyList()), null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Invalid or expired JWT token");
                return;
            }

        } catch (Exception e) {
            sendErrorResponse(response, HttpStatus.UNAUTHORIZED, "Authentication error: " + e.getMessage());
            return;
        }

        chain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("status", status.value());
        errorDetails.put("error", status.getReasonPhrase());
        errorDetails.put("message", message);
        errorDetails.put("path", response.getHeader("Referer")); // Optional: Shows the request path

        ObjectMapper objectMapper = new ObjectMapper();
        response.getWriter().write(objectMapper.writeValueAsString(errorDetails));
    }
}

