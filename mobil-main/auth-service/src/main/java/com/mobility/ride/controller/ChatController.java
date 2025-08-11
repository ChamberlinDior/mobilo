// ============================================================================
//  FILE : src/main/java/com/mobility/ride/controller/ChatController.java
//  v2025‑10‑12 – rider‑send bug fix (senderId forcé côté serveur)
//               + vérif participant + diffusion WS centrale
// ============================================================================
package com.mobility.ride.controller;

import com.mobility.ride.dto.ChatMessageRequest;
import com.mobility.ride.model.ChatMessage;
import com.mobility.ride.service.ChatService;
import com.mobility.ride.service.RideParticipationService;
import com.mobility.auth.service.AuthenticatedUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

/**
 * Messagerie in‑app entre passager et conducteur.
 *
 * <ul>
 *   <li>REST : listing + POST fallback (devices sans WebSocket).</li>
 *   <li>WebSocket (STOMP) : /app/chat.send.{rideId} → /topic/ride/{rideId}/chat.</li>
 *   <li>Sécurité : seuls les participants (rider | driver) peuvent publier ou lire.</li>
 * </ul>
 */
@Tag(name = "Chat", description = "In‑app chat between rider & driver")
@RestController
@RequestMapping("/api/v1/rides/{rideId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService              chatService;
    private final RideParticipationService participationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final SimpMessagingTemplate    simp;

    /* ═══════════ 1) Récupération du thread (REST) ═══════════ */
    @Operation(summary = "List chat messages",
            responses = @ApiResponse(
                    responseCode = "200",
                    content = @Content(schema = @Schema(implementation = ChatMessage.class))))
    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessage>> list(
            @PathVariable Long rideId,
            HttpServletRequest req) {

        Long userId = authenticatedUserService.getAuthenticatedUserId(
                req.getHeader("Authorization"));

        if (!participationService.isParticipantOfRide(rideId, userId))
            return ResponseEntity.status(403).build();

        return ResponseEntity.ok(chatService.listMessages(rideId));
    }

    /* ═══════════ 2) Envoi message (REST fallback) ═══════════ */
    @Operation(summary = "Send a chat message (HTTP fallback)",
            responses = @ApiResponse(
                    responseCode = "201",
                    content = @Content(schema = @Schema(implementation = ChatMessage.class))))
    @PostMapping("/messages")
    public ResponseEntity<ChatMessage> send(
            @PathVariable Long rideId,
            @RequestBody @Valid ChatMessageRequest body,
            HttpServletRequest req) {

        Long userId = authenticatedUserService.getAuthenticatedUserId(
                req.getHeader("Authorization"));

        if (!participationService.isParticipantOfRide(rideId, userId))
            return ResponseEntity.status(403).build();

        // on force l’expéditeur côté serveur — ignore body.senderId
        ChatMessage saved = chatService.sendMessage(
                rideId,
                new ChatMessageRequest(userId, body.text())
        );

        // diffusion temps‑réel
        simp.convertAndSend("/topic/ride/" + rideId + "/chat", saved);
        return ResponseEntity.status(201).body(saved);
    }

    /* ═══════════ 3) Envoi message (WebSocket STOMP) ═══════════ */
    @MessageMapping("/chat.send.{rideId}")          // client → /app/chat.send.{rideId}
    public void wsSend(@DestinationVariable Long rideId,
                       @Valid ChatMessageRequest body,
                       Principal principal) {

        Long userId = Long.valueOf(principal.getName());   // JWT sub = internal id

        if (!participationService.isParticipantOfRide(rideId, userId))
            return;    // 403 silencieux sur WS

        ChatMessage saved = chatService.sendMessage(
                rideId,
                new ChatMessageRequest(userId, body.text())   // id imposé serveur
        );

        simp.convertAndSend("/topic/ride/" + rideId + "/chat", saved);
    }
}
