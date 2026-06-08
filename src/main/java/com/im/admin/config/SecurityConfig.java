package com.im.admin.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate5.jakarta.Hibernate5JakartaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Set;

import java.util.List;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;

@Configuration
public class SecurityConfig {

    private static final Set<String> PATHS_VIEWABLE_WITHOUT_AUTHENTICATION = Set.of("/images/**","/webjars/**", "cache",
            "/auth/login", "/auth/verify-otp", "/auth/resend-otp", "/auth/set-password", "/auth/forgot-password",
            "/auth/verify-forgot-mpin-otp", "/api/tenant/config",
            "/auth/register", "/api/aes/generate-token", "/api/aes/validate-token", "/api/pg/generate-token",
            "/api/pg/callback/runpaisa", "/api/pg/callback/nixasoft", "/api/pg/callback/slpe", "/api/pg/callback/unisus",
            "/api/payout/callback", "/auth/forget-verify-otp", "/api/aes/encrypt-credentials",
            "/api/aes/decrypt-credentials", "/api/pg/callback/razorpay", "/api/pg/callback/cashfree",
            "/v3/api-docs", "/v3/api-docs/**", "/swagger-ui", "/swagger-ui/**", "/swagger-ui.html");

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())

                //  DEBUG FILTER
                .addFilterBefore(new OncePerRequestFilter() {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    FilterChain filterChain)
                            throws ServletException, IOException {

                        System.out.println("==== REQUEST START ====");
                        System.out.println("URI      : " + request.getRequestURI());
                        System.out.println("METHOD   : " + request.getMethod());
                        System.out.println("AUTH HDR : " + request.getHeader("Authorization"));

                        filterChain.doFilter(request, response);

                        System.out.println("STATUS   : " + response.getStatus());
                        System.out.println("==== REQUEST END ====\n");
                    }
                }, UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth

                        //  WebSocket allow
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers("/ws/info/**").permitAll()
                        .requestMatchers("/sockjs/**").permitAll()

                        //  Public APIs
                        .requestMatchers(PATHS_VIEWABLE_WITHOUT_AUTHENTICATION.toArray(new String[0])).permitAll()

                        //  Others
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public ObjectMapper objectMapper() {

        ObjectMapper mapper = new ObjectMapper();

        mapper.registerModule(new JavaTimeModule());

        mapper.registerModule(
                new Hibernate5JakartaModule()
                        .disable(Hibernate5JakartaModule.Feature.USE_TRANSIENT_ANNOTATION)
        );

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        return mapper;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "https://*.decipay.in"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true); //  IMPORTANT

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

}



