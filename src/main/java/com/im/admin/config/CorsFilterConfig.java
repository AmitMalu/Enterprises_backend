package com.im.admin.config;

/**
 * @author SHUBHAM KHOPADE
 */
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsFilterConfig {

    /** Extra allowed origins per environment, e.g. the demo server (comma-separated). */
    @Value("${app.cors.extra-origins:}")
    private List<String> extraOrigins;

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // Allow specific origins
        List<String> allowedOrigins = new ArrayList<>(Arrays.asList(
                "http://localhost:5173",
                "http://localhost:5174",
                "https://uat.decipay.in",
                "http://10.1.3.4",
                "http://10.1.3.5",
                "http://202.191.129.23",
                "http://202.191.129.24",
                "https://portal.nfttech.in",        // Portal public URL
                "https://admin.nfttech.in",         // Admin public URL
                "https://portal.lakshaype.com",     // Lakshaype Public URL
                "https://portal.nexatrust.in",      //Bagade Public URL
                "https://portal.lbvyapar.com",      //LogiBudha Public URL
                "http://192.168.1.127:4173",         //Amaan public IP
                "https://portal.mintpe.in"           //Mint pay
        ));
        extraOrigins.stream().filter(o -> !o.isBlank()).forEach(allowedOrigins::add);
        config.setAllowedOrigins(allowedOrigins);

        // Allow all HTTP methods
        config.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"
        ));

        // Allow all headers
        config.setAllowedHeaders(Arrays.asList("*"));

        // Expose headers to client
        config.setExposedHeaders(Arrays.asList(
                "Authorization", "Content-Type", "X-Requested-With"
        ));

        // Allow credentials (cookies, authorization headers)
        config.setAllowCredentials(true);

        // Cache preflight requests for 1 hour
        config.setMaxAge(3600L);

        // Apply to all endpoints
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
