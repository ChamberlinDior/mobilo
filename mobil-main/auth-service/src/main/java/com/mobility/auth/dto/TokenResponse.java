package com.mobility.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.OffsetDateTime;

/**
 * Réponse standard d’authentification / rafraîchissement.
 * <ul>
 *   <li>Conforme aux pratiques Uber / Lyft / OAuth2.</li>
 *   <li>Inclut les champs optionnels utiles pour la télémétrie ou la MFA.</li>
 *   <li>Aucune donnée sensible (secret, hash, claims internes).</li>
 * </ul>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenResponse {

    /* ═══════════ Jeton d’accès ═══════════ */
    private final String tokenType;        // "Bearer"
    private final String accessToken;
    private final Long   expiresIn;        // secondes jusqu’à expiration
    private final String scope;            // "read write" (optionnel)

    /* ═══════════ Jeton de rafraîchissement ═══════════ */
    private final String refreshToken;
    private final Long   refreshExpiresIn;

    /* ═══════════ Sécurité & session ═══════════ */
    private final String  sessionId;       // UUID session côté backend (optionnel)
    private final String  deviceId;        // identifiant device mobile (optionnel)
    private final Boolean mfaRequired;     // true si une 2FA doit s’enchaîner

    /* ═══════════ Métadonnées ═══════════ */
    private final OffsetDateTime issuedAt;

    /* ═══════════ Profil résumé ═══════════ */
    private final UserResponse user;
}
