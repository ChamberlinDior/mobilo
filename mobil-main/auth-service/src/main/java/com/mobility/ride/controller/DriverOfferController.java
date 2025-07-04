// ────────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/controller/DriverOfferController.java
//  v2025-08-10 – flux chauffeur : rides / parcels / scheduled + actions
// ────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.controller;

import com.mobility.ride.dto.RideOfferDto;
import com.mobility.ride.service.DriverFeedService;
import com.mobility.ride.service.DriverRideActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * End-points REST consommés par l’application « Driver ».
 * - Récupération d’offres (rides, colis, planifiées)
 * - Acceptation / refus d’une offre
 */
@Tag(
        name        = "Driver – Ride offers",
        description = "Nearby ride/parcel offers & ride actions for drivers"
)
@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverOfferController {

    private final DriverFeedService       feedService;
    private final DriverRideActionService actionService;

    private static final double DEF_RADIUS_KM = 3.0;

    /* ────────────────────────────────────────────────
       1) COURSES IMMÉDIATES (status = REQUESTED)
       GET /api/v1/drivers/open-rides
       ──────────────────────────────────────────────── */
    @Operation(
            summary   = "Open ride requests near the driver",
            responses = @ApiResponse(responseCode = "200",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = RideOfferDto.class))))
    )
    @GetMapping("/open-rides")
    public ResponseEntity<List<RideOfferDto>> openRides(
            @RequestParam @NotNull  @DecimalMin("-90.0")  @DecimalMax("90.0")   Double lat,
            @RequestParam @NotNull  @DecimalMin("-180.0") @DecimalMax("180.0")  Double lng,
            @RequestParam(defaultValue = "3.0")
            @DecimalMin("0.1")       @DecimalMax("20.0")   Double radiusKm
    ) {
        return ResponseEntity.ok(
                feedService.findOpenRides(lat, lng,
                        radiusKm == null ? DEF_RADIUS_KM : radiusKm));
    }

    /* Alias rétro-compatibilité : /offers */
    @GetMapping("/offers")
    public ResponseEntity<List<RideOfferDto>> aliasOffers(
            @RequestParam Double lat,
            @RequestParam Double lng,
            @RequestParam(defaultValue = "3.0") Double radiusKm) {
        return openRides(lat, lng, radiusKm);
    }

    /* ────────────────────────────────────────────────
       2) COLIS À LIVRER
       GET /api/v1/drivers/open-parcels
       ──────────────────────────────────────────────── */
    @Operation(summary = "Parcel / delivery offers near the driver")
    @GetMapping("/open-parcels")
    public List<RideOfferDto> openParcels(
            @RequestParam @NotNull  @DecimalMin("-90.0")  @DecimalMax("90.0")   Double lat,
            @RequestParam @NotNull  @DecimalMin("-180.0") @DecimalMax("180.0")  Double lng,
            @RequestParam(defaultValue = "3.0")
            @DecimalMin("0.1")       @DecimalMax("20.0")   Double radiusKm
    ) {
        return feedService.findOpenParcels(lat, lng,
                radiusKm == null ? DEF_RADIUS_KM : radiusKm);
    }

    /* ────────────────────────────────────────────────
       3) COURSES PLANIFIÉES (< 30 min)
       GET /api/v1/drivers/scheduled-rides
       ──────────────────────────────────────────────── */
    @Operation(summary = "Scheduled rides starting soon (≤ 30 min)")
    @GetMapping("/scheduled-rides")
    public List<RideOfferDto> scheduledRides(
            @RequestParam @NotNull  @DecimalMin("-90.0")  @DecimalMax("90.0")   Double lat,
            @RequestParam @NotNull  @DecimalMin("-180.0") @DecimalMax("180.0")  Double lng,
            @RequestParam(defaultValue = "3.0")
            @DecimalMin("0.1")       @DecimalMax("20.0")   Double radiusKm
    ) {
        return feedService.findScheduledRides(lat, lng,
                radiusKm == null ? DEF_RADIUS_KM : radiusKm);
    }

    /* ────────────────────────────────────────────────
       4) ACTIONS DRIVER : ACCEPT / DECLINE
       ──────────────────────────────────────────────── */
    @Operation(summary = "Driver accepts a ride request",
            responses = @ApiResponse(responseCode = "204"))
    @PostMapping("/rides/{rideId}/accept")
    public ResponseEntity<Void> accept(@PathVariable Long rideId) {
        actionService.accept(rideId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Driver declines a ride request",
            responses = @ApiResponse(responseCode = "204"))
    @PostMapping("/rides/{rideId}/decline")
    public ResponseEntity<Void> decline(@PathVariable Long rideId) {
        actionService.decline(rideId);
        return ResponseEntity.noContent().build();
    }
}
