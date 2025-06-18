// ─────────────────────────────────────────────────────────────────────────────
// DTO : SplitBillRequest
// Partage d’addition (split-fare)
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.dto;

import jakarta.validation.constraints.*;

public record SplitBillRequest(
        @NotNull Long payerId,
        @Min(1) @Max(99) Integer sharePct  // % à la charge du payerId
) {}
