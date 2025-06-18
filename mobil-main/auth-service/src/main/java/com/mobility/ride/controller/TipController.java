// ============================================================================
//  FILE    : TipController.java
// ----------------------------------------------------------------------------
package com.mobility.ride.controller;

import com.mobility.ride.dto.SplitBillRequest;
import com.mobility.ride.dto.TipRequest;
import com.mobility.ride.model.PaymentSplit;
import com.mobility.ride.model.Tip;
import com.mobility.ride.service.TipService;
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

/**
 * Pourboires & partage de l’addition sur une même course (ride).
 *
 * Désormais sécurisé :
 * 1. On extrait l’utilisateur authentifié via JWT (AuthenticatedUserService).
 * 2. On vérifie que payerId dans le corps correspond à l’utilisateur authentifié.
 * 3. On vérifie que cet utilisateur participe à la course (RideParticipationService).
 */
@Tag(name = "Tips & Split-Bill", description = "Tips and payment splitting for a ride")
@RestController
@RequestMapping("/api/v1/rides/{rideId}")
@RequiredArgsConstructor
public class TipController {

    private final TipService tipService;
    private final RideParticipationService participationService;
    private final AuthenticatedUserService authenticatedUserService;

    /* ═══════════ Ajout d’un pourboire ═══════════ */
    @Operation(
            summary = "Send a tip",
            responses = @ApiResponse(
                    responseCode = "201",
                    description = "Tip accepted",
                    content = @Content(schema = @Schema(implementation = Tip.class))
            )
    )
    @PostMapping("/tips")
    public ResponseEntity<Tip> tip(
            @PathVariable Long rideId,
            @RequestBody @Valid TipRequest body,
            HttpServletRequest request
    ) {
        // 1) Extrait l’ID interne de l’utilisateur authentifié
        Long userId = authenticatedUserService.getAuthenticatedUserId(
                request.getHeader("Authorization")
        );

        // 2) Vérifie que payerId dans le corps correspond à l’utilisateur authentifié
        if (!userId.equals(body.payerId())) {
            return ResponseEntity.status(403).build();
        }

        // 3) Vérifie que l’utilisateur participe à la course
        if (!participationService.isParticipantOfRide(rideId, userId)) {
            return ResponseEntity.status(403).build();
        }

        // 4) Crée et renvoie le pourboire
        Tip tip = tipService.addTip(rideId, body);
        return ResponseEntity.status(201).body(tip);
    }

    /* ═══════════ Split-bill ═══════════ */
    @Operation(
            summary = "Split the fare",
            responses = @ApiResponse(
                    responseCode = "201",
                    description = "Split created",
                    content = @Content(schema = @Schema(implementation = PaymentSplit.class))
            )
    )
    @PostMapping("/split-bill")
    public ResponseEntity<PaymentSplit> split(
            @PathVariable Long rideId,
            @RequestBody @Valid SplitBillRequest body,
            HttpServletRequest request
    ) {
        // 1) Extrait l’ID interne de l’utilisateur authentifié
        Long userId = authenticatedUserService.getAuthenticatedUserId(
                request.getHeader("Authorization")
        );

        // 2) Vérifie que payerId dans le corps correspond à l’utilisateur authentifié
        if (!userId.equals(body.payerId())) {
            return ResponseEntity.status(403).build();
        }

        // 3) Vérifie que l’utilisateur participe à la course
        if (!participationService.isParticipantOfRide(rideId, userId)) {
            return ResponseEntity.status(403).build();
        }

        // 4) Crée et renvoie le split-bill
        PaymentSplit split = tipService.splitBill(rideId, body);
        return ResponseEntity.status(201).body(split);
    }
}
