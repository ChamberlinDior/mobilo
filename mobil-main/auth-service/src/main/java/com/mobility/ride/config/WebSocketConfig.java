// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/config/WebSocketConfig.java
//  v2025-10-11 – handshake JWT interceptor branché
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.config;

import com.mobility.auth.ws.JwtHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /* Intercepteur JWT ➜ Principal */
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry cfg) {
        cfg.enableSimpleBroker("/topic");                // destinations WS sortantes
        cfg.setApplicationDestinationPrefixes("/app");   // destinations WS entrantes
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry reg) {
        reg.addEndpoint("/ws")
                .addInterceptors(jwtHandshakeInterceptor)     // ⬅️  handshake JWT → Principal
                .setAllowedOriginPatterns("*");               // CORS libre (prod : restreindre)
        // Optionnel : SockJS fallback
        // reg.addEndpoint("/ws").withSockJS();
    }
}
