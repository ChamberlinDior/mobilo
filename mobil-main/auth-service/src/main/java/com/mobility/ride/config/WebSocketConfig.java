// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/config/WebSocketConfig.java
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry cfg) {
        cfg.enableSimpleBroker("/topic");
        cfg.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry reg) {
        reg.addEndpoint("/ws").setAllowedOriginPatterns("*");
    }
}
