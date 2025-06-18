// ─────────────────────────────────────────────────────────────────────────────
// PACKAGE : com.mobility.ride.dto
// ----------------------------------------------------------------------------
package com.mobility.ride.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Payload POST /chat/rides/{id}/messages */
public record ChatMessageRequest(
        @NotNull Long   senderId,
        @NotBlank String text
) {}
