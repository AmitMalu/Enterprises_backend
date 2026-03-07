package com.im.admin.util;

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
}
