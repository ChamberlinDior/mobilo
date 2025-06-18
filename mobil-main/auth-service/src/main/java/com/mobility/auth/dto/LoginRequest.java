package com.mobility.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * <h2>Payload « Login / Authenticate »</h2>
 *
 * <p>Couverture stricte des champs exigés par le flux OAuth-like :</p>
 * <ul>
 *   <li><strong>Identifiant</strong> : adresse e-mail en format RFC 5322.</li>
 *   <li><strong>Secret</strong> : mot de passe brut (sera immédiatement
 *       haché puis oublié).</li>
 *   <li><strong>deviceId</strong> (facultatif) : remonte l’ID du terminal
 *       mobile pour la télémétrie (push, anti-fraude, MFA…).</li>
 * </ul>
 *
 * <p>Implémenté en <em>record</em> Java 21 – immuable, thread-safe,
 * (dé)sérialisation auto via Jackson.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record LoginRequest(

        /* ═══════════ Identité ═══════════ */
        @Email
        @NotBlank
        @Size(max = 160)
        String email,

        /* ═══════════ Secret ═══════════ */
        @Size(min = 8, max = 72,
                message = "Password must be between 8 and 72 characters")
        String password,

        /* ═══════════ Métadonnées device (optionnel) ═══════════ */
        @Size(max = 64)
        String deviceId

) {}
