package com.scs.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    /** Extra allowed origins per environment, e.g. the demo server (comma-separated). */
    @Value("${app.cors.extra-origins:}")
    private String[] extraOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "https://uat.decipay.in",
                        "http://10.1.3.4",                  // Portal frontend
                        "http://10.1.3.5",                  // Backend itself
                        "http://202.191.129.23",            // Portal public IP
                        "http://202.191.129.24",            // Backend public IP
                        "https://portal1.utsabpay.com",        // Portal public URL
                        "https://admin1.utsabpay.com",         // Admin public URL
                        "https://portal.lakshaype.com",     // Lakshaype Public URL
                        "https://portal.nexatrust.in",      //Bagade Public URL
                        "https://portal.lbvyapar.com",      //LogiBudha Public URL
                        "http://192.168.1.127:4173",         //Amaan public IP
                        "https://portal.mintpe.in"           //Mint pay

                )
                .allowedOriginPatterns(extraOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)  // Change to true for Authorization headers
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("==== STATIC RESOURCE HANDLER LOADED ====");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/opt/scs/uploads/");
    }
}