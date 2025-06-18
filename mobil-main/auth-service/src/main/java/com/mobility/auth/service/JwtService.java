package com.mobility.auth.service;

import com.mobility.auth.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

/**
 * JWT Service – implémentation validée sur <strong>jjwt-api 0.12.5</strong>.
 *
 * <p>On utilise l’API <code>parser().verifyWith(key)…build().parseSignedClaims()</code>
 * (présente depuis la 0.10).<br>
 * La signature est HS256 ; il suffit de changer <code>ALG</code> et la clé
 * si vous voulez passer en RS256.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    /* ─────────── Paramètres externes ─────────── */
    @Value("${app.jwt.secret}")                 // clé 256 bits Base64
    private String secretB64;

    @Value("${app.jwt.issuer:mobility-auth}")
    private String issuer;

    @Value("${app.jwt.clock-skew-sec:60}")
    private long clockSkewSec;

    /* ─────────── Internes ─────────── */
    private SecretKey secretKey;
    private static final SignatureAlgorithm ALG = SignatureAlgorithm.HS256;

    /* ─────────── Observabilité ─────────── */
    private final MeterRegistry metrics;
    private Counter invalidTokCounter;

    @PostConstruct
    void init() {
        secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretB64));
        invalidTokCounter = Counter.builder("auth.jwt.invalid").register(metrics);
    }

    /* ═══════════ API publique ═══════════ */

    /** Génère un access-token signé. */
    public String generateToken(User user, OffsetDateTime expiresAtUtc) {

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())                // jti
                .subject(user.getExternalUid())                  // sub
                .issuer(issuer)                                  // iss
                .issuedAt(Date.from(now.toInstant()))            // iat
                .expiration(Date.from(expiresAtUtc.toInstant())) // exp
                /* Custom claims */
                .claim("uid",   user.getId())
                .claim("role",  user.getPrimaryRole().name())
                .claim("scope", buildScope(user))
                /* Signature */
                .signWith(secretKey, ALG)
                .compact();
    }

    /**
     * Valide la signature & les dates, renvoie directement les <em>Claims</em>.
     * Lève {@link JwtException} si token invalide/expiré ou signature incorrecte.
     */
    public Claims validate(String token) throws JwtException {

        Jwt<?, Claims> signed =
                Jwts.parser()                      // ↔ JwtParserBuilder
                        .verifyWith(secretKey)         // clé HS256
                        .requireIssuer(issuer)
                        .clockSkewSeconds(clockSkewSec)
                        .build()
                        .parseSignedClaims(token);     // retourne Jwt<?, Claims>

        return signed.getPayload();
    }

    /** Extrait le <code>uid</code> public ou {@code null} si invalide. */
    public String extractUserUid(String token) {
        try {
            return validate(token).getSubject();
        } catch (JwtException ex) {
            invalidTokCounter.increment();
            log.warn("JWT invalid : {}", ex.getMessage());
            return null;
        }
    }

    /* ═══════════ Helpers ═══════════ */

    private String buildScope(User user) {
        // exemple : "driver mfa read write"
        Set<String> scopes = new LinkedHashSet<>();
        scopes.add(user.getPrimaryRole().name().toLowerCase());
        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) scopes.add("mfa");
        scopes.addAll(List.of("read", "write"));
        return String.join(" ", scopes);
    }
}
