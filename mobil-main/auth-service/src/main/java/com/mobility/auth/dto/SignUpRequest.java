package com.mobility.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.*;
import lombok.Builder;

/**
 * <h2>Payload « Sign-up / Create Account »</h2>
 * <p>
 *   • Strictement les informations indispensables pour ouvrir un compte
 *   (pattern utilisé par Uber / Lyft).<br>
 *   • Toutes les contraintes de validation sont portées par
 *   <em>jakarta.validation</em> pour un feedback immédiat côté API.<br>
 *   • Implémenté en <strong>record</strong> Java 21 💡 : immuable, thread-safe,
 *   sérialisation Jackson automatique.<br>
 *   • Les champs facultatifs (<em>referralCode</em>, <em>deviceId</em>)
 *   sont marqués <code>@JsonInclude(…NON_NULL)</code> → absents du JSON si null.
 * </p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder                     // permet SignUpRequest.builder()… même sur un record
public record SignUpRequest(

        /* ═══════════ Identité obligatoire ═══════════ */
        @Email
        @NotBlank
        @Size(max = 160)
        String email,

        @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$",
                message = "Phone number must be E.164 (min 8, max 15 digits)")
        String phoneNumber,

        @Size(min = 8, max = 72,
                message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank
        @Size(max = 60)
        String firstName,

        @NotBlank
        @Size(max = 60)
        String lastName,

        /* ═══════════ Options avancées ═══════════ */
        @Pattern(regexp = "^[A-Z0-9]{6}$",
                message = "Referral code must be 6 alphanumeric uppercase characters")
        String referralCode,             // parrainage (facultatif)

        @Size(max = 64)
        String deviceId                  // identifiant device mobile pour la télémétrie (facultatif)

) {}
