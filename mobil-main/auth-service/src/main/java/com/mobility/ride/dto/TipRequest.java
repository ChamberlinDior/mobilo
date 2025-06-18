// ─────────────────────────────────────────────────────────────────────────────
// DTO : TipRequest
// Ajout d’un pourboire sur une course donnée
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Payload JSON pour <code>POST /rides/{rideId}/tips</code>.
 * <ul>
 *   <li><strong>payerId</strong> – auteur du pourboire</li>
 *   <li><strong>amount</strong>  – montant &gt; 0</li>
 *   <li><strong>currency</strong> – ISO 4217 (ex. “XAF”, “EUR”)</li>
 * </ul>
 */
public record TipRequest(
        @NotNull Long payerId,
        @NotNull @Positive BigDecimal amount,
        @NotNull @Size(min = 3, max = 3) String currency
) {}
