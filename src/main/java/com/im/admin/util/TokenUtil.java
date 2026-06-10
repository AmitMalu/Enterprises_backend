package com.im.admin.util;

import com.im.admin.request.UserContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Component
public class TokenUtil {

    public String extractHierarchyId() {

        HttpServletRequest request =
                ((ServletRequestAttributes)
                        RequestContextHolder.getRequestAttributes())
                        .getRequest();

        String token = request.getHeader("Authorization");

        if (token == null || !token.contains("|")) {
            throw new RuntimeException("Invalid token");
        }

        // If header is like: Bearer token
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // Token format:
        // hierarchyId|salt|iv|encryptedToken

        String[] parts = token.split("\\|");

        if (parts.length < 4) {
            throw new RuntimeException("Invalid token format");
        }

        return parts[0];
    }

    public UserContext getUserContext() {

        HttpServletRequest request =
                ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes())
                        .getRequest();

        String token = request.getHeader("Authorization");

        if (token == null || token.isBlank()) {
            throw new RuntimeException("Missing token");
        }

        token = token.trim();

        if (token.startsWith("Bearer ")) {
            token = token.substring(7).trim();
        }

        String[] parts = token.split("\\|");

        if (parts.length < 4) {
            throw new RuntimeException("Invalid token format");
        }

        // Remove leading/trailing spaces, tabs, newlines, etc.
        String hierarchyId = parts[0] == null
                ? null
                : parts[0].trim().replaceAll("\\s+", "");

        if (hierarchyId == null || hierarchyId.isBlank()) {
            throw new RuntimeException("Invalid hierarchy id");
        }

        String[] hierarchyParts = hierarchyId.split("-");

        if (hierarchyParts.length < 2) {
            throw new RuntimeException("Invalid hierarchy format: " + hierarchyId);
        }

        String role = hierarchyParts[1].trim().toUpperCase();

        UserContext ctx = new UserContext();

        ctx.setHierarchyId(hierarchyId);

        // Only AD and SA are admins
        ctx.setAdmin("AD".equals(role) || "SA".equals(role));

        return ctx;
    }

}
