// ─────────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/RideUserService.java
//  v2025-09-05 – compatible avec findByUid **et** findByExternalUid
//               + docs & messages d’erreur unifiés
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.auth.model.User;
import com.mobility.auth.repository.UserRepository;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;

/**
 *Service utilitaire : convertit le JWT Bearer reçu par l’app mobile
 *(header <b>Authorization: Bearer …</b>) en identifiant interne <code>Long</code>.
 *
 * <p>Tout le module « Ride » peut ainsi récupérer l’id sans ré-implémenter
 * toute la logique d’authentification Spring Security.</p>
 */
@Service("rideUserService")
public class RideUserService {

    private final JwtDecoder     jwtDecoder;
    private final UserRepository userRepository;

    public RideUserService(@Value("${app.jwt.secret}") String authSecretB64,
                           UserRepository userRepository) {

        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(authSecretB64));
        this.jwtDecoder     = NimbusJwtDecoder.withSecretKey(key).build();
        this.userRepository = userRepository;
    }

    /**
     * @param bearerToken header complet « Bearer eyJ… »
     * @return primary-key interne (table <i>users</i>)
     * @throws IllegalArgumentException si le header est manquant / invalide
     */
    public Long getAuthenticatedUserId(String bearerToken) {

        /* ─── 1) Contrôle du header ───────────────────────────── */
        if (bearerToken == null || !bearerToken.startsWith("Bearer "))
            throw new IllegalArgumentException("MISSING_OR_INVALID_AUTHORIZATION_HEADER");

        String token = bearerToken.substring(7);

        /* ─── 2) Décodage JWT ────────────────────────────────── */
        Jwt jwt;
        try {
            jwt = jwtDecoder.decode(token);
        } catch (Exception ex) {
            throw new IllegalArgumentException("INVALID_JWT_TOKEN", ex);
        }

        String externalUid = jwt.getSubject();   // champ « sub »
        if (externalUid == null || externalUid.isBlank())
            throw new IllegalArgumentException("JWT_MISSING_SUBJECT");

        /* ─── 3) Recherche en base ─────────────────────────────
               findByUid = nouvelle méthode “officielle”
               findByExternalUid = alias legacy (gardé pour compat)
         */
        return userRepository.findByUid(externalUid)
                .or(() -> userRepository.findByExternalUid(externalUid))
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
    }
}
