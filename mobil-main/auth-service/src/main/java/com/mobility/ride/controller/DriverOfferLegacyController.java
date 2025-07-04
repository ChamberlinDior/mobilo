// ──────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/controller/DriverOfferLegacyController.java
//  v2025-08-02 – Alias historique  /api/v1/driver/offers
// ──────────────────────────────────────────────────────────────────────────
package com.mobility.ride.controller;

import com.mobility.ride.dto.RideResponse;
import com.mobility.ride.service.RideQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur *legacy* : maintient la route
 *     GET /api/v1/driver/offers
 * pour ne pas casser les versions existantes de l’app chauffeur.
 */
@RestController
@RequestMapping("/api/v1/driver")          // ← singulier (legacy)
@RequiredArgsConstructor
public class DriverOfferLegacyController {

    private final RideQueryService rideQueryService;

    @Operation(
            summary   = "Legacy – open ride requests near driver",
            responses = @ApiResponse(
                    responseCode = "200",
                    description  = "Array of RideResponse",
                    content      = @Content(schema = @Schema(implementation = RideResponse.class))
            )
    )
    @GetMapping("/offers")
    public ResponseEntity<List<RideResponse>> offers(
            @RequestParam
            @NotNull  @DecimalMin("-90")  @DecimalMax("90")   Double lat,
            @RequestParam
            @NotNull  @DecimalMin("-180") @DecimalMax("180")  Double lng,
            @RequestParam(defaultValue = "3.0")
            @DecimalMin("0.1")  @DecimalMax("20.0") Double radiusKm
    ) {
        return ResponseEntity.ok(rideQueryService.listOpen(lat, lng, radiusKm));
    }
}
