package com.scs.admin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Extra allowed origins per environment, e.g. the demo server (comma-separated). */
    @Value("${app.cors.extra-origins:}")
    private String[] extraOrigins;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "https://uat.decipay.in",
                        "http://10.1.3.4",
                        "http://10.1.3.5",
                        "http://202.191.129.23",
                        "http://202.191.129.24",
                        "https://portal1.utsabpay.com",        // Portal public URL
                        "https://admin1.utsabpay.com",         // Admin public URL
                        "https://portal.lakshaype.com",     // Lakshaype Public URL,
                        "https://portal.nexatrust.in",      //Bagade Public URL
                        "http://192.168.1.127:4173",         //Amaan public IP
                        "https://portal.mintpe.in"           //Mint pay

                )
                .setAllowedOriginPatterns(extraOrigins)
                .withSockJS();

        // Add endpoint without SockJS for simpler clients
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "https://uat.decipay.in",
                        "http://10.1.3.4"
                )
                .setAllowedOriginPatterns(extraOrigins);
    }
}