// src/main/java/com/mobility/ride/service/RideUserService.java
package com.mobility.ride.service;

import com.mobility.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

/**
 * Service utilitaire pour extraire l’ID interne du rider (ou driver)
 * authentifié à partir du JWT Bearer, spécifique au module Ride.
 */
@Service("rideUserService")
public class RideUserService {

    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;

    public RideUserService(
            @Value("${app.jwt.secret}") String authSecretB64,
            UserRepository userRepository
    ) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(authSecretB64));
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(key).build();
        this.userRepository = userRepository;
    }

    /**
     * Extrait l’ID interne (Long) de l’utilisateur à partir du header Authorization.
     */
    public Long getAuthenticatedUserId(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("MISSING_OR_INVALID_AUTHORIZATION_HEADER");
        }
        String token = bearerToken.substring(7);

        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (Exception ex) {
            throw new IllegalArgumentException("INVALID_JWT_TOKEN", ex);
        }

        String externalUid = jwt.getSubject();
        if (externalUid == null || externalUid.isBlank()) {
            throw new IllegalArgumentException("JWT_MISSING_SUBJECT");
        }

        return userRepository.findByExternalUid(externalUid)
                .map(u -> u.getId())
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
    }
}
