// src/main/java/com/mobility/auth/dto/PaymentMethodRequest.java
package com.mobility.auth.dto;

import com.mobility.auth.model.enums.PaymentProvider;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Requête d’ajout / MAJ d’un moyen de paiement.
 *
 *  • provider : Stripe / PayPal / CASH…
 *  • type     : card | cash   (logique de facturation)
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentMethodRequest {

    /** Prestataire / source de paiement. */
    @NotNull
    private PaymentProvider provider;

    /** card | cash  (→ colonne `type`). */
    @NotBlank
    @Pattern(regexp = "^(card|cash)$", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "doit valoir «card» ou «cash»")
    private String type;

    /** Jeton opaque du PSP (pm_xxx, token PayPal, etc.) — facultatif si cash. */
    @Size(max = 128)
    private String token;

    /* ─── Détails carte – optionnels suivant provider ─── */

    @Size(max = 32)
    private String brand;           // VISA, PAYPAL, AIRTEL…

    @Size(min = 4, max = 4)
    private String last4;

    @Min(1) @Max(12)
    private Integer expMonth;

    @Min(2024)
    private Integer expYear;

    /** Si vrai : rend ce moyen par défaut. */
    private Boolean makeDefault = Boolean.FALSE;
}
