package com.mobility.auth.ws;

import io.jsonwebtoken.Claims;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Collections;
import java.util.Map;

/**
 * Intercepte le handshake WebSocket et transforme le JWT
 * « Authorization: Bearer xxx » en Principal Spring.
 */
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtDecoder decoder;

    public JwtHandshakeInterceptor(JwtDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest request,
                                   @NonNull ServerHttpResponse response,
                                   @NonNull WebSocketHandler wsHandler,
                                   @NonNull Map<String, Object> attrs) {

        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Jwt jwt = decoder.decode(token);
                Claims claims = (Claims) jwt.getClaims();
                String userId = claims.getSubject();          // convention : sub = id
                UsernamePasswordAuthenticationToken principal =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                Collections.singletonList(new SimpleGrantedAuthority("USER")));
                attrs.put("principal", principal);
            } catch (Exception ignore) { /* handshake allowed but unauthenticated */ }
        }
        return true; // toujours laisser passer : contrôle dans l’appli
    }

    @Override
    public void afterHandshake(@NonNull ServerHttpRequest request,
                               @NonNull ServerHttpResponse response,
                               @NonNull WebSocketHandler wsHandler,
                               Exception ex) { /* no-op */ }
}
