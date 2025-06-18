// ============================================================================
//  FILE    : CancellationController.java
// ----------------------------------------------------------------------------
package com.mobility.ride.controller;

import com.mobility.ride.service.CancellationPenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Gestion des annulations & pénalités.
 */
@Tag(name = "Cancellation", description = "Ride cancellation and automatic fees")
@RestController
@RequestMapping("/api/v1/rides/{rideId}")
@RequiredArgsConstructor
public class CancellationController {

    private final CancellationPenaltyService cancellationService;

    @Operation(summary = "Cancel a ride",
            responses = @ApiResponse(responseCode = "204", description = "Ride cancelled"))
    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(@PathVariable Long rideId) {
        cancellationService.cancelRide(rideId);
        return ResponseEntity.noContent().build();
    }
}