package com.im.admin.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class WebConfig implements WebMvcConfigurer {

    private static final List<String> ALLOWED_ORIGINS = List.of("*");

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("*") // Allow Vite React app
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false);  // if you’re using cookies or Authorization header
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:/opt/im/uploads/"); // include trailing slash
    }


//    @Bean
//    public FilterRegistrationBean<OncePerRequestFilter> rawBodyFilter() {
//
//        FilterRegistrationBean<OncePerRequestFilter> registration =
//                new FilterRegistrationBean<>();
//
//        registration.setFilter(new OncePerRequestFilter() {
//            @Override
//            protected void doFilterInternal(HttpServletRequest request,
//                                            HttpServletResponse response,
//                                            FilterChain filterChain)
//                    throws ServletException, IOException {
//
//                ContentCachingRequestWrapper wrappedRequest =
//                        new ContentCachingRequestWrapper(request);
//
//                filterChain.doFilter(wrappedRequest, response);
//            }
//        });
//
//        registration.addUrlPatterns("/api/pg/callback/cashfree");
//        registration.setOrder(1); // ensure it runs early
//
//        return registration;
//    }


}
