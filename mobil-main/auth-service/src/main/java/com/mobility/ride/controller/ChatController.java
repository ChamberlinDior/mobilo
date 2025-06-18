// ============================================================================
//  FILE    : ChatController.java
// ----------------------------------------------------------------------------
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
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Messagerie in-app entre passager et conducteur.
 *
 * Désormais sécurisé :
 * 1. On extrait l’utilisateur authentifié via JWT (AuthenticatedUserService).
 * 2. On vérifie que cet utilisateur est bien rider ou driver de la course (RideParticipationService).
 * 3. Pour l’envoi, on s’assure aussi que senderId dans le corps correspond à l’utilisateur authentifié.
 */
@Tag(name = "Chat", description = "In-app chat between rider & driver")
@RestController
@RequestMapping("/api/v1/rides/{rideId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final RideParticipationService participationService;
    private final AuthenticatedUserService authenticatedUserService;

    /* ═══════════ Récupération du thread ═══════════ */
    @Operation(
            summary = "List chat messages",
            responses = @ApiResponse(
                    responseCode = "200",
                    description = "Chat thread",
                    content = @Content(schema = @Schema(implementation = ChatMessage.class))
            )
    )
    @GetMapping("/messages")
    public ResponseEntity<List<ChatMessage>> list(
            @PathVariable Long rideId,
            HttpServletRequest request
    ) {
        // 1) Extrait l’ID interne de l’utilisateur authentifié
        Long userId = authenticatedUserService.getAuthenticatedUserId(
                request.getHeader("Authorization")
        );

        // 2) Vérifie que l’utilisateur participe à la course (rider ou driver)
        if (!participationService.isParticipantOfRide(rideId, userId)) {
            return ResponseEntity.status(403).build();
        }

        // 3) Récupère et retourne la liste des messages
        List<ChatMessage> thread = chatService.listMessages(rideId);
        return ResponseEntity.ok(thread);
    }

    /* ═══════════ Envoi d’un message ═══════════ */
    @Operation(
            summary = "Send a chat message",
            responses = @ApiResponse(
                    responseCode = "201",
                    description = "Message sent",
                    content = @Content(schema = @Schema(implementation = ChatMessage.class))
            )
    )
    @PostMapping("/messages")
    public ResponseEntity<ChatMessage> send(
            @PathVariable Long rideId,
            @RequestBody @Valid ChatMessageRequest body,
            HttpServletRequest request
    ) {
        // 1) Extrait l’ID interne de l’utilisateur authentifié
        Long userId = authenticatedUserService.getAuthenticatedUserId(
                request.getHeader("Authorization")
        );

        // 2) Vérifie que senderId dans le corps correspond à l’utilisateur authentifié
        if (!userId.equals(body.senderId())) {
            return ResponseEntity.status(403).build();
        }

        // 3) Vérifie que l’utilisateur participe à la course
        if (!participationService.isParticipantOfRide(rideId, userId)) {
            return ResponseEntity.status(403).build();
        }

        // 4) Enregistre et renvoie le message
        ChatMessage msg = chatService.sendMessage(rideId, body);
        return ResponseEntity.status(201).body(msg);
    }
}
