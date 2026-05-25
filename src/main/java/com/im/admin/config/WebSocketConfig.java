package com.im.admin.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

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
                        "https://portal.nfttech.in",       // Portal public URL
                        "https://admin.nfttech.in"      // Admin public URL
                )
                .withSockJS();

        // Add endpoint without SockJS for simpler clients
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                        "http://localhost:5173",
                        "http://localhost:5174",
                        "https://uat.decipay.in",
                        "http://10.1.3.4"
                );
    }
}