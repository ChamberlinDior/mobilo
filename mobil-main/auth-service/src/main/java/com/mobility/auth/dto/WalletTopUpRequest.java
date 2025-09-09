/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/dto/WalletTopUpRequest.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Demande de recharge du wallet.
 * Règles:
 *  - amount   : > 0.00
 *  - currency : NULL autorisé (devise déduite côté service). Si présent → ISO-4217 (3 lettres).
 *  - paymentMethodId : requis, identifiant numérique d’un moyen de paiement existant.
 */
@Getter @Setter
public class WalletTopUpRequest {

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be >= 0.01")
    private BigDecimal amount;

    /** Devise optionnelle. Si absente : résolue via devise du wallet / géoloc. */
    @Size(min = 3, max = 3, message = "currency must be 3 letters (ISO-4217)")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "currency must be a 3-letter ISO code")
    private String currency; // nullable OK

    /** paymentMethodId retourné par /payment-methods (id DB numérique). */
    @NotBlank(message = "paymentMethodId is required")
    @Pattern(regexp = "^\\d{1,19}$", message = "paymentMethodId must be a numeric id")
    private String paymentMethodId;
}
