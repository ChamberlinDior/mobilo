// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/controller/RideController.java
//  v2025-06-15 – « Your Trips / History & Re-planification »
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.controller;

import com.mobility.ride.dto.RequestRideRequest;
import com.mobility.ride.dto.RideResponse;
import com.mobility.ride.dto.ScheduleRideRequest;
import com.mobility.ride.service.RideService;
import com.mobility.ride.service.RideUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * End-points REST relatifs aux courses (« rides »).
 *
 * <p>Fonctionnalités exposées :</p>
 * <ul>
 *   <li>Demande immédiate :             <b>POST /rides/request</b></li>
 *   <li>Planification future :          <b>POST /rides/schedule</b></li>
 *   <li>Liste des rides planifiés :     <b>GET  /rides/scheduled</b></li>
 *   <li>Historique des rides :          <b>GET  /rides/history</b></li>
 *   <li>Re-planification :              <b>PATCH /rides/{id}/reschedule</b></li>
 *   <li>Consultation individuelle :     <b>GET  /rides/{id}</b></li>
 * </ul>
 *
 * <p><b>IMPORTANT :</b> tous les chemins fixes (<code>/scheduled</code>,
 * <code>/history</code>, …) sont déclarés <em>avant</em> le catch-all
 * <code>/{rideId:\d+}</code> pour éviter les collisions.</p>
 */
@RestController
@RequestMapping("/api/v1/rides")
@RequiredArgsConstructor
public class RideController {

    private final RideService     rideService;
    private final RideUserService rideUserService;

    /* ═══════════════════════════════════════════════════════════
       1) DEMANDE IMMÉDIATE
       ═══════════════════════════════════════════════════════════ */
    @PostMapping("/request")
    public ResponseEntity<RideResponse> requestRide(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid RequestRideRequest request
    ) {
        Long riderId = rideUserService.getAuthenticatedUserId(authHeader);

        RequestRideRequest sanitized = RequestRideRequest.builder()
                .riderId     (riderId)
                .pickupLat   (request.getPickupLat())
                .pickupLng   (request.getPickupLng())
                .dropoffLat  (request.getDropoffLat())
                .dropoffLng  (request.getDropoffLng())
                .productType (request.getProductType())
                // ─── Ajout des deux champs manquants ─────────
                .totalFare   (request.getTotalFare())
                .currency    (request.getCurrency())
                // ────────────────────────────────────────────
                .options     (request.getOptions())
                .build();

        RideResponse resp = rideService.requestRide(sanitized);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /* ═══════════════════════════════════════════════════════════
       2) PLANIFICATION
       ═══════════════════════════════════════════════════════════ */
    @PostMapping("/schedule")
    public ResponseEntity<RideResponse> scheduleRide(
            @RequestHeader("Authorization") String authHeader,
            @RequestBody @Valid ScheduleRideRequest body
    ) {
        Long riderId = rideUserService.getAuthenticatedUserId(authHeader);

        ScheduleRideRequest sanitized = ScheduleRideRequest.builder()
                .riderId         (riderId)
                .pickupLat       (body.getPickupLat())
                .pickupLng       (body.getPickupLng())
                .dropoffLat      (body.getDropoffLat())
                .dropoffLng      (body.getDropoffLng())
                .productType     (body.getProductType())
                .options         (body.getOptions())
                .scheduledAt     (body.getScheduledAt())
                .paymentMethodId (body.getPaymentMethodId())
                .totalFare       (body.getTotalFare())
                .currency        (body.getCurrency())
                .build();

        RideResponse resp = rideService.scheduleRide(sanitized);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    /* ═══════════════════════════════════════════════════════════
       3) LISTE DES RIDES PLANIFIÉS (onglet « À venir »)
       ═══════════════════════════════════════════════════════════ */
    @GetMapping("/scheduled")
    public ResponseEntity<List<RideResponse>> listScheduled(
            @RequestHeader("Authorization") String authHeader) {

        Long riderId = rideUserService.getAuthenticatedUserId(authHeader);
        return ResponseEntity.ok(rideService.listScheduled(riderId));
    }

    /* ═══════════════════════════════════════════════════════════
       4) HISTORIQUE DES RIDES  (onglet « Historique »)
       ═══════════════════════════════════════════════════════════ */
    @GetMapping("/history")
    public ResponseEntity<List<RideResponse>> listHistory(
            @RequestHeader("Authorization") String authHeader) {

        Long riderId = rideUserService.getAuthenticatedUserId(authHeader);
        return ResponseEntity.ok(rideService.listHistory(riderId));
    }

    /* ═══════════════════════════════════════════════════════════
       5) RE-PLANIFICATION (déplacement de l’heure)
       ═══════════════════════════════════════════════════════════ */
    @PatchMapping("/{rideId:\\d+}/reschedule")
    public ResponseEntity<Void> reschedule(
            @PathVariable Long rideId,
            @RequestParam("scheduledAt")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime newTs) {

        rideService.reschedule(rideId, newTs);
        return ResponseEntity.noContent().build();
    }

    /* ═══════════════════════════════════════════════════════════
       6) CONSULTATION INDIVIDUELLE
       ═══════════════════════════════════════════════════════════ */
    @GetMapping("/{rideId:\\d+}")
    public ResponseEntity<RideResponse> getRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.getRide(rideId));
    }
}
