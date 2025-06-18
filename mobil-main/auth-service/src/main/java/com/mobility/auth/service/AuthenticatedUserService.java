// src/main/java/com/mobility/auth/service/AuthenticatedUserService.java
package com.mobility.auth.service;

import com.mobility.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.util.Map;

/**
 * <h2>AuthenticatedUserService</h2>
 *
 * <p>
 * Extrait l’ID interne (Long) de l’utilisateur authentifié à partir du token JWT.
 * On récupère la claim "sub" (externalUid) depuis l’objet org.springframework.security.oauth2.jwt.Jwt,
 * puis on recherche l’utilisateur en base via UserRepository.findByExternalUid(...).
 * </p>
 */
@Service
public class AuthenticatedUserService {

    private final UserRepository userRepository;
    private final JwtDecoder jwtDecoder;

    /**
     * @param userRepository  Repository JPA permettant de récupérer l’utilisateur par externalUid
     * @param secretB64       Clé HS256 encodée en Base64 (configurée dans application.properties)
     */
    public AuthenticatedUserService(
            UserRepository userRepository,
            @Value("${app.jwt.secret}") String secretB64
    ) {
        this.userRepository = userRepository;

        // Décodage de la clé Base64 pour construire un JwtDecoder HS256
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretB64));
        this.jwtDecoder = NimbusJwtDecoder.withSecretKey(key).build();
    }

    /**
     * Extrait l’ID interne (Long) depuis le header Authorization (Bearer <token>).
     *
     * @param bearerToken Valeur brute du header "Authorization" (ex. : "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
     * @return l’ID interne (Long) de l’utilisateur
     * @throws IllegalArgumentException si le header est manquant/mal formé,
     *                                  le JWT invalide, ou si aucun utilisateur n’est trouvé.
     */
    public Long getAuthenticatedUserId(String bearerToken) {
        if (bearerToken == null || !bearerToken.startsWith("Bearer ")) {
            throw new IllegalArgumentException("MISSING_OR_INVALID_AUTHORIZATION_HEADER");
        }
        String token = bearerToken.substring(7);

        Jwt jwt;
        try {
            jwt = (Jwt) jwtDecoder.decode(token);
        } catch (Exception ex) {
            throw new IllegalArgumentException("INVALID_JWT_TOKEN");
        }

        // La méthode getSubject() renvoie la claim "sub" directement
        String externalUid = jwt.getSubject();
        if (externalUid == null || externalUid.isBlank()) {
            throw new IllegalArgumentException("JWT_MISSING_SUBJECT");
        }

        // Recherche l’utilisateur en base via externalUid (String) → récupère son ID interne (Long)
        return userRepository.findByExternalUid(externalUid)
                .map(user -> user.getId())
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
    }
}
