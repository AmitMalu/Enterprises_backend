package com.im.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "https://uat.decipay.in",
                        "http://10.1.3.4",           // Portal frontend
                        "http://10.1.3.5",           // Backend itself
                        "http://202.191.129.23",      // Portal public IP
                        "http://202.191.129.24",       // Backend public IP
                        "https://portal.nfttech.in",       // Portal public URL
                        "https://admin.nfttech.in"      // Admin public URL
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)  // Change to true for Authorization headers
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        System.out.println("==== STATIC RESOURCE HANDLER LOADED ====");

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/opt/nft/uploads/");
    }
}