// ============================================================================
//  FILE : src/main/java/com/mobility/ride/dto/ChatMessageRequest.java
//  v2025‑10‑12 – senderId optional (removal @NotNull) – déterminé côté serveur
// ============================================================================
package com.mobility.ride.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload JSON pour :
 *   • POST  /api/v1/rides/{rideId}/chat/messages   (fallback HTTP)
 *   • WS    /app/chat.send.{rideId}                (STOMP)
 *
 * <p>⚠️  Depuis la v2025‑10‑12, le champ <b>senderId</b> est facultatif :
 * le contrôleur injecte systématiquement l’identifiant authentifié afin
 * d’éviter toute usurpation ou incohérence client.</p>
 *
 * Exemple :
 * <pre>
 * {
 *   "text": "Je suis à l’angle de la rue."
 * }
 * </pre>
 */
public record ChatMessageRequest(
        Long   senderId,          // peut être null → valeur forcée par le serveur
        @NotBlank String text
) {}
