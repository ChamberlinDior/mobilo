// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/controller/RideController.java
//  v2025-10-07 – endpoint /complete corrigé (plus de toBuilder)
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.controller;

import com.mobility.ride.dto.RequestRideRequest;
import com.mobility.ride.dto.RideResponse;
import com.mobility.ride.dto.ScheduleRideRequest;
import com.mobility.ride.model.DeliveryZone;
import com.mobility.ride.service.RideLifecycleService;
import com.mobility.ride.service.RideService;
import com.mobility.ride.service.RideUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RideController {

    private final RideService          rideService;
    private final RideUserService      rideUserService;
    private final RideLifecycleService rideLifecycleSvc;

    /* ═════════════════════ 1) DEMANDE IMMÉDIATE ═══════════════════════ */
    @PostMapping("/rides/request")
    public ResponseEntity<RideResponse> requestRide(
            @RequestHeader("Authorization") String auth,
            @RequestBody @Valid RequestRideRequest req) {

        Long riderId = rideUserService.getAuthenticatedUserId(auth);

        DeliveryZone zone = req.getDeliveryZone() != null
                ? req.getDeliveryZone()
                : DeliveryZone.LOCAL;
        if (zone != DeliveryZone.LOCAL && req.getWeightKg() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Weight (kg) must be provided for interurban and international deliveries");
        }

        RequestRideRequest sanitized = RequestRideRequest.builder()
                .riderId     (riderId)
                .pickupLat   (req.getPickupLat())
                .pickupLng   (req.getPickupLng())
                .dropoffLat  (req.getDropoffLat())
                .dropoffLng  (req.getDropoffLng())
                .productType (req.getProductType())
                .weightKg    (req.getWeightKg())
                .deliveryZone(zone)
                .totalFare   (req.getTotalFare())
                .currency    (req.getCurrency())
                .options     (req.getOptions())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rideService.requestRide(sanitized));
    }

    /* ═════════════════════ 2) PLANIFICATION ═══════════════════════════ */
    @PostMapping("/rides/schedule")
    public ResponseEntity<RideResponse> scheduleRide(
            @RequestHeader("Authorization") String auth,
            @RequestBody @Valid ScheduleRideRequest body) {

        Long riderId = rideUserService.getAuthenticatedUserId(auth);

        DeliveryZone zone = body.getDeliveryZone() != null
                ? body.getDeliveryZone()
                : DeliveryZone.LOCAL;
        if (zone != DeliveryZone.LOCAL && body.getWeightKg() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Weight (kg) must be provided for interurban and international deliveries");
        }

        ScheduleRideRequest sanitized = ScheduleRideRequest.builder()
                .riderId        (riderId)
                .pickupLat      (body.getPickupLat())
                .pickupLng      (body.getPickupLng())
                .dropoffLat     (body.getDropoffLat())
                .dropoffLng     (body.getDropoffLng())
                .productType    (body.getProductType())
                .options        (body.getOptions())
                .weightKg       (body.getWeightKg())
                .deliveryZone   (zone)
                .scheduledAt    (body.getScheduledAt())
                .paymentMethodId(body.getPaymentMethodId())
                .totalFare      (body.getTotalFare())
                .currency       (body.getCurrency())
                .build();

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(rideService.scheduleRide(sanitized));
    }

    /* ═════════════════════ 3) LISTE PLANIFIÉES ════════════════════════ */
    @GetMapping("/rides/scheduled")
    public ResponseEntity<List<RideResponse>> listScheduled(
            @RequestHeader("Authorization") String auth) {

        Long riderId = rideUserService.getAuthenticatedUserId(auth);
        return ResponseEntity.ok(rideService.listScheduled(riderId));
    }

    /* ═════════════════════ 4) HISTORIQUE RIDER ════════════════════════ */
    @GetMapping("/rides/history")
    public ResponseEntity<List<RideResponse>> listHistory(
            @RequestHeader("Authorization") String auth) {

        Long riderId = rideUserService.getAuthenticatedUserId(auth);
        return ResponseEntity.ok(rideService.listHistory(riderId));
    }

    /* ═════════════════════ 5) RE-PLANIFICATION ════════════════════════ */
    @PatchMapping("/rides/{rideId:\\d+}/reschedule")
    public ResponseEntity<Void> reschedule(
            @PathVariable Long rideId,
            @RequestParam("scheduledAt")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime newTs) {

        rideService.reschedule(rideId, newTs);
        return ResponseEntity.noContent().build();
    }

    /* ═════════════════════ 6) DÉTAIL D’UNE COURSE ═════════════════════ */
    @GetMapping("/rides/{rideId:\\d+}")
    public ResponseEntity<RideResponse> getRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.getRide(rideId));
    }

    /* ═════════════════════ 7) ALIAS SINGULIER (mobile) ════════════════ */
    @GetMapping("/ride/{id}")
    public ResponseEntity<RideResponse> getRideAlias(@PathVariable Long id) {
        return getRide(id);
    }

    /* ═════════════════════ 8) TERMINER LA COURSE (driver) ═════════════ */
    @PatchMapping("/rides/{rideId:\\d+}/complete")
    public ResponseEntity<Void> completeRide(
            @PathVariable Long rideId,
            @RequestParam  BigDecimal finalFare) {

        /* Appel direct au service de cycle de vie.
           Si vous souhaitez restreindre l’accès au driver, placez ici
           une vérification avec rideUserService + rideService.getRide(…). */
        rideLifecycleSvc.completeRide(rideId, finalFare);
        return ResponseEntity.noContent().build();
    }
}
