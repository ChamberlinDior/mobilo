// ─────────────────────────────────────────────────────────────────────────────
// PACKAGE : com.mobility.ride.dto
// ----------------------------------------------------------------------------
package com.mobility.ride.dto;

import java.math.BigDecimal;

/** Réponse POST /rides/{id}/cancel */
public record CancellationPenaltyResponse(
        BigDecimal fee,
        boolean    charged
) {}
