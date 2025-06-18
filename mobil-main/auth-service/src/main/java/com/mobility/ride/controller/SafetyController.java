// ============================================================================
//  FILE    : SafetyController.java
// ----------------------------------------------------------------------------
package com.mobility.ride.controller;

import com.mobility.ride.dto.SOSRequest;
import com.mobility.ride.service.SafetyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Sécurité : bouton SOS, PIN, incidents…
 */
@Tag(name = "Safety", description = "SOS button, PIN verification, incidents")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SafetyController {

    private final SafetyService safetyService;

    /* ═══════════ SOS ═══════════ */
    @Operation(summary = "Trigger SOS",
            responses = @ApiResponse(responseCode = "204", description = "SOS sent"))
    @PostMapping("/safety/sos")
    public ResponseEntity<Void> sos(@RequestBody @Valid SOSRequest body) {
        safetyService.triggerSos(body);
        return ResponseEntity.noContent().build();
    }

    /* ═══════════ Vérification PIN ═══════════ */
    @Operation(summary = "Verify ride PIN",
            responses = @ApiResponse(responseCode = "204", description = "PIN OK"))
    @PostMapping("/rides/{rideId}/pin/verify")
    public ResponseEntity<Void> verifyPin(
            @PathVariable Long rideId,
            @RequestParam @NotBlank String pin) {
        safetyService.verifyPin(rideId, pin);
        return ResponseEntity.noContent().build();
    }
}