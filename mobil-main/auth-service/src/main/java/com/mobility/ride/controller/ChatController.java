// ============================================================================
//  FILE : src/main/java/com/mobility/ride/controller/ChatController.java
//  v2025-10-11 – ajout WebSocket STOMP /chat.send.{rideId}
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
 * Messagerie in-app entre passager et conducteur.
 *
 * <ul>
 *   <li>REST : listing + post fallback (devices sans WS).</li>
 *   <li>WebSocket (STOMP) : /app/chat.send.{rideId} ➜ /topic/ride/{rideId}/chat.</li>
 *   <li>Sécurité : seul un participant (rider | driver) peut publier ou lire.</li>
 * </ul>
 */
@Tag(name = "Chat", description = "In-app chat between rider & driver")
@RestController
@RequestMapping("/api/v1/rides/{rideId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService              chatService;
    private final RideParticipationService participationService;
    private final AuthenticatedUserService authenticatedUserService;
    private final SimpMessagingTemplate    simp;                  // ← NEW

    /* ═══════════ Récupération du thread (REST) ═══════════ */
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

    /* ═══════════ Envoi d’un message (REST fallback) ═══════════ */
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

        if (!userId.equals(body.senderId())
                || !participationService.isParticipantOfRide(rideId, userId))
            return ResponseEntity.status(403).build();

        ChatMessage saved = chatService.sendMessage(rideId, body);
        // diffuse aux sockets connectées
        simp.convertAndSend("/topic/ride/" + rideId + "/chat", saved);
        return ResponseEntity.status(201).body(saved);
    }

    /* ═══════════ Envoi d’un message (WebSocket STOMP) ═══════════ */
    @MessageMapping("/chat.send.{rideId}")          // côté client : /app/chat.send.{rideId}
    public void wsSend(@DestinationVariable Long rideId,
                       @Valid ChatMessageRequest body,
                       Principal principal) {

        Long userId = Long.valueOf(principal.getName());  // JWT sub = internal id

        if (!userId.equals(body.senderId())
                || !participationService.isParticipantOfRide(rideId, userId))
            return;   // 403 silencieux côté WS

        ChatMessage saved = chatService.sendMessage(rideId, body);
        simp.convertAndSend("/topic/ride/" + rideId + "/chat", saved);
    }
}
