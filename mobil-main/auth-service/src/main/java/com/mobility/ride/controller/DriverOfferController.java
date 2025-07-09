// ──────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/controller/DriverOfferController.java
//  v2025-09-07 – ajout gestion 409 + documentation OpenAPI
// ──────────────────────────────────────────────────────────────
package com.mobility.ride.controller;

import com.mobility.ride.dto.RideOfferDto;
import com.mobility.ride.service.DriverFeedService;
import com.mobility.ride.service.DriverRideActionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * End-points REST consommés par l’application « Driver ».
 *
 * <ul>
 *   <li>Feed d’offres (rides, colis, planifiées)</li>
 *   <li>Actions temps-réel sur une course : accept, decline, en-route, arrive, start, finish</li>
 * </ul>
 *
 * <p><b>driverId</b> est automatiquement déduit du JWT (claim
 * <code>"uid"</code> par défaut) ; aucun paramètre supplémentaire n’est
 * nécessaire côté mobile.</p>
 */
@Tag(name = "Driver – Ride offers",
        description = "Nearby ride / parcel offers & ride actions for drivers")
@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
public class DriverOfferController {

    /* ─── Services ───────────────────────────────────────────── */
    private final DriverFeedService       feedService;
    private final DriverRideActionService actionService;

    private static final double DEF_RADIUS_KM = 3.0;

    /* ════════════════════════════════════════════════════════════
       1) FEED : Rides / Parcels / Scheduled
       ════════════════════════════════════════════════════════════ */

    @Operation(summary = "Open ride requests near the driver",
            responses = @ApiResponse(responseCode = "200",
                    content = @Content(array = @ArraySchema(
                            schema = @Schema(implementation = RideOfferDto.class)))))
    @GetMapping("/open-rides")
    public ResponseEntity<List<RideOfferDto>> openRides(
            @RequestParam @NotNull  @DecimalMin("-90")  @DecimalMax("90")   Double lat,
            @RequestParam @NotNull  @DecimalMin("-180") @DecimalMax("180")  Double lng,
            @RequestParam(defaultValue = "3.0")
            @DecimalMin("0.1")       @DecimalMax("20")   Double radiusKm) {

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

    @Operation(summary = "Parcel / delivery offers near the driver")
    @GetMapping("/open-parcels")
    public List<RideOfferDto> openParcels(
            @RequestParam @NotNull  @DecimalMin("-90")  @DecimalMax("90")   Double lat,
            @RequestParam @NotNull  @DecimalMin("-180") @DecimalMax("180")  Double lng,
            @RequestParam(defaultValue = "3.0")
            @DecimalMin("0.1")       @DecimalMax("20")   Double radiusKm) {

        return feedService.findOpenParcels(lat, lng,
                radiusKm == null ? DEF_RADIUS_KM : radiusKm);
    }

    @Operation(summary = "Scheduled rides starting soon (≤ 30 min)")
    @GetMapping("/scheduled-rides")
    public List<RideOfferDto> scheduledRides(
            @RequestParam @NotNull  @DecimalMin("-90")  @DecimalMax("90")   Double lat,
            @RequestParam @NotNull  @DecimalMin("-180") @DecimalMax("180")  Double lng,
            @RequestParam(defaultValue = "3.0")
            @DecimalMin("0.1")       @DecimalMax("20")   Double radiusKm) {

        return feedService.findScheduledRides(lat, lng,
                radiusKm == null ? DEF_RADIUS_KM : radiusKm);
    }

    /* ════════════════════════════════════════════════════════════
       2) ACTIONS CHAUFFEUR
       ════════════════════════════════════════════════════════════ */

    /** Helper : extrait l’ID numérique du chauffeur depuis le JWT. */
    private Long driverId(@AuthenticationPrincipal Jwt jwt) {
        // Convention : claim "uid" = id interne
        return jwt.getClaim("uid");
    }

    /** Acceptation d’une offre. */
    @PostMapping("/rides/{rideId}/accept")
    @Operation(summary = "Driver accepts a ride request",
            responses = {
                    @ApiResponse(responseCode = "204"),
                    @ApiResponse(responseCode = "409",
                            description = "Ride already accepted or cancelled")
            })
    public ResponseEntity<Void> accept(@PathVariable Long rideId,
                                       @AuthenticationPrincipal Jwt jwt) {

        try {
            actionService.accept(rideId, driverId(jwt));
            return ResponseEntity.noContent().build();         // 204 OK
        } catch (IllegalStateException ex) {
            // ► Course déjà prise ou annulée : 409 CONFLICT
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    ex.getMessage(), ex);
        }
    }

    /** Refus d’une offre. */
    @PostMapping("/rides/{rideId}/decline")
    @Operation(summary = "Driver declines a ride request",
            responses = @ApiResponse(responseCode = "204"))
    public ResponseEntity<Void> decline(@PathVariable Long rideId,
                                        @AuthenticationPrincipal Jwt jwt) {

        actionService.decline(rideId, driverId(jwt));
        return ResponseEntity.noContent().build();
    }

    /** Chauffeur démarre la navigation vers le pickup. */
    @PostMapping("/rides/{rideId}/en-route")
    public ResponseEntity<Void> startEnRoute(@PathVariable Long rideId) {
        actionService.startEnRoute(rideId);
        return ResponseEntity.noContent().build();
    }

    /** Chauffeur arrivé au point de prise en charge. */
    @PostMapping("/rides/{rideId}/arrive")
    public ResponseEntity<Void> arrive(@PathVariable Long rideId) {
        actionService.arrive(rideId);
        return ResponseEntity.noContent().build();
    }

    /** Passager embarqué : début de la course. */
    @PostMapping("/rides/{rideId}/start")
    public ResponseEntity<Void> startRide(@PathVariable Long rideId) {
        actionService.startRide(rideId);
        return ResponseEntity.noContent().build();
    }

    /** Fin de course : distance & durée réelles transmises. */
    @PostMapping("/rides/{rideId}/finish")
    public ResponseEntity<Void> finish(@PathVariable Long  rideId,
                                       @RequestParam  @Positive double distanceKm,
                                       @RequestParam  @Positive long   durationSec) {

        actionService.finish(rideId, distanceKm, durationSec);
        return ResponseEntity.noContent().build();
    }
}
