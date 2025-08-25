// ─────────────────────────────────────────────────────────────
// FILE : src/main/java/com/mobility/ride/controller/RideController.java
// v2025-10-13 – routes complètes pour Rides feed (active/upcoming/history),
//               endpoints driver miroir, “current”, offres proches, transitions,
//               currency côté serveur, règles poids colis, alias compat.
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.controller;

import com.mobility.ride.dto.RequestRideRequest;
import com.mobility.ride.dto.RideResponse;
import com.mobility.ride.dto.ScheduleRideRequest;
import com.mobility.ride.model.DeliveryZone;
import com.mobility.ride.service.CurrencyResolver;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class RideController {

    private final RideService          rideService;
    private final RideUserService      rideUserService;
    private final RideLifecycleService rideLifecycleSvc;
    private final CurrencyResolver     currencyResolver;

    /* ═════════════════════ 1) DEMANDE IMMÉDIATE (rider) ═════════════ */
    @PostMapping("/rides/request")
    public ResponseEntity<RideResponse> requestRide(
            @RequestHeader("Authorization") String auth,
            @RequestBody @Valid RequestRideRequest req) {

        Long riderId = rideUserService.getAuthenticatedUserId(auth);
        DeliveryZone zone = Optional.ofNullable(req.getDeliveryZone()).orElse(DeliveryZone.LOCAL);

        // Règle poids pour livraisons (≠ LOCAL)
        if (zone != DeliveryZone.LOCAL) {
            if (req.getWeightKg() == null || req.getWeightKg().compareTo(new BigDecimal("0.1")) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Weight (kg) must be ≥ 0.1 for interurban and international deliveries");
            }
        }

        // Devise côté serveur (pickup → pays), fallback: pricing.default.currency
        String resolvedCurrency = currencyResolver.resolve(req.getPickupLat(), req.getPickupLng(), null);

        RequestRideRequest sanitized = RequestRideRequest.builder()
                .riderId     (riderId)
                .pickupLat   (req.getPickupLat())
                .pickupLng   (req.getPickupLng())
                .dropoffLat  (req.getDropoffLat())
                .dropoffLng  (req.getDropoffLng())
                .productType (req.getProductType())
                .options     (req.getOptions())
                .weightKg    (req.getWeightKg())
                .deliveryZone(zone)
                .totalFare   (req.getTotalFare())
                .currency    (resolvedCurrency)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.requestRide(sanitized));
    }

    /* ═════════════════════ 2) PLANIFICATION (rider) ════════════════ */
    @PostMapping("/rides/schedule")
    public ResponseEntity<RideResponse> scheduleRide(
            @RequestHeader("Authorization") String auth,
            @RequestBody @Valid ScheduleRideRequest body) {

        Long riderId = rideUserService.getAuthenticatedUserId(auth);
        DeliveryZone zone = Optional.ofNullable(body.getDeliveryZone()).orElse(DeliveryZone.LOCAL);

        if (zone != DeliveryZone.LOCAL) {
            if (body.getWeightKg() == null || body.getWeightKg().compareTo(new BigDecimal("0.1")) < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Weight (kg) must be ≥ 0.1 for interurban and international deliveries");
            }
        }

        String resolvedCurrency = currencyResolver.resolve(body.getPickupLat(), body.getPickupLng(), null);

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
                .currency       (resolvedCurrency)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(rideService.scheduleRide(sanitized));
    }

    /* ═════════════ 3) FEEDS RIDER: ACTIVE / UPCOMING / HISTORY / CURRENT ═════════════ */
    @GetMapping("/rides/active")
    public ResponseEntity<List<RideResponse>> listActiveRider(
            @RequestHeader("Authorization") String auth) {
        Long riderId = rideUserService.getAuthenticatedUserId(auth);
        return ResponseEntity.ok(rideService.listActiveForRider(riderId));
    }

    @GetMapping({"/rides/upcoming", "/rides/scheduled"}) // alias
    public ResponseEntity<List<RideResponse>> listUpcomingRider(
            @RequestHeader("Authorization") String auth) {
        Long riderId = rideUserService.getAuthenticatedUserId(auth);
        return ResponseEntity.ok(rideService.listScheduledForRider(riderId));
    }

    @GetMapping("/rides/history")
    public ResponseEntity<List<RideResponse>> listHistoryRider(
            @RequestHeader("Authorization") String auth) {
        Long riderId = rideUserService.getAuthenticatedUserId(auth);
        return ResponseEntity.ok(rideService.listHistoryForRider(riderId));
    }

    @GetMapping("/rides/current")
    public ResponseEntity<RideResponse> currentRider(
            @RequestHeader("Authorization") String auth) {
        Long riderId = rideUserService.getAuthenticatedUserId(auth);
        return rideService.currentForRider(riderId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /* ═════════════ 4) FEEDS DRIVER: ACTIVE / UPCOMING / HISTORY / CURRENT ═════════════ */
    @GetMapping("/rides/driver/active")
    public ResponseEntity<List<RideResponse>> listActiveDriver(
            @RequestHeader("Authorization") String auth) {
        Long driverId = rideUserService.getAuthenticatedUserId(auth);
        return ResponseEntity.ok(rideService.listActiveForDriver(driverId));
    }

    @GetMapping("/rides/driver/upcoming")
    public ResponseEntity<List<RideResponse>> listUpcomingDriver(
            @RequestHeader("Authorization") String auth) {
        Long driverId = rideUserService.getAuthenticatedUserId(auth);
        return ResponseEntity.ok(rideService.listScheduledForDriver(driverId));
    }

    @GetMapping("/rides/driver/history")
    public ResponseEntity<List<RideResponse>> listHistoryDriver(
            @RequestHeader("Authorization") String auth) {
        Long driverId = rideUserService.getAuthenticatedUserId(auth);
        return ResponseEntity.ok(rideService.listHistoryForDriver(driverId));
    }

    @GetMapping("/rides/driver/current")
    public ResponseEntity<RideResponse> currentDriver(
            @RequestHeader("Authorization") String auth) {
        Long driverId = rideUserService.getAuthenticatedUserId(auth);
        return rideService.currentForDriver(driverId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    /* ═════════════ 5) OFFRES PROCHES (driver) ═════════════ */
    @GetMapping("/rides/open-near")
    public ResponseEntity<List<RideResponse>> listOpenNear(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(name = "radiusKm", defaultValue = "5") double radiusKm) {
        return ResponseEntity.ok(rideService.listOpenNear(lat, lng, radiusKm));
    }

    /* ═════════════ 6) RE-PLANIFICATION ═════════════ */
    @PatchMapping("/rides/{rideId:\\d+}/reschedule")
    public ResponseEntity<Void> reschedule(
            @PathVariable Long rideId,
            @RequestParam("scheduledAt") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime newTs) {
        rideService.reschedule(rideId, newTs);
        return ResponseEntity.noContent().build();
    }

    /* ═════════════ 7) DÉTAIL D’UNE COURSE ═════════════ */
    @GetMapping("/rides/{rideId:\\d+}")
    public ResponseEntity<RideResponse> getRide(@PathVariable Long rideId) {
        return ResponseEntity.ok(rideService.getRide(rideId));
    }

    /* Alias singulier (mobile legacy) */
    @GetMapping("/ride/{id}")
    public ResponseEntity<RideResponse> getRideAlias(@PathVariable Long id) {
        return getRide(id);
    }

    /* ═════════════ 8) TRANSITIONS DRIVER (assign/en-route/arrived/start/cancel/complete) ═════════════ */
    @PatchMapping("/rides/{rideId:\\d+}/assign")
    public ResponseEntity<Void> assignRide(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long rideId) {
        Long driverId = rideUserService.getAuthenticatedUserId(auth);
        boolean ok = rideService.assignIfAvailable(rideId, driverId);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PatchMapping("/rides/{rideId:\\d+}/en-route")
    public ResponseEntity<Void> markEnRoute(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long rideId) {
        Long driverId = rideUserService.getAuthenticatedUserId(auth);
        boolean ok = rideService.markEnRoute(rideId, driverId);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PatchMapping("/rides/{rideId:\\d+}/arrived")
    public ResponseEntity<Void> markArrived(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long rideId) {
        Long driverId = rideUserService.getAuthenticatedUserId(auth);
        boolean ok = rideService.markArrived(rideId, driverId);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PatchMapping("/rides/{rideId:\\d+}/start")
    public ResponseEntity<Void> startRide(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long rideId) {
        Long driverId = rideUserService.getAuthenticatedUserId(auth);
        boolean ok = rideService.startRide(rideId, driverId);
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PatchMapping("/rides/{rideId:\\d+}/cancel")
    public ResponseEntity<Void> cancelRide(
            @PathVariable Long rideId,
            @RequestParam(name = "reason", required = false) String reason) {
        boolean ok = rideService.cancelRide(rideId, Optional.ofNullable(reason).orElse("unspecified"));
        return ok ? ResponseEntity.noContent().build() : ResponseEntity.status(HttpStatus.CONFLICT).build();
    }

    @PatchMapping("/rides/{rideId:\\d+}/complete")
    public ResponseEntity<Void> completeRide(
            @RequestHeader("Authorization") String auth,
            @PathVariable Long rideId,
            @RequestParam BigDecimal finalFare) {
        Long driverId = rideUserService.getAuthenticatedUserId(auth);
        rideService.driverComplete(rideId, driverId, finalFare);
        return ResponseEntity.noContent().build();
    }

    /* ═════════════ 9) FIN LEGACY (si des clients anciens l’utilisent) ═════════════ */
    @PatchMapping("/rides/{rideId:\\d+}/finish")
    public ResponseEntity<Void> finishLegacy(
            @PathVariable Long rideId,
            @RequestParam BigDecimal finalFare) {
        // NB: ne vérifie pas le driverId (legacy)
        rideLifecycleSvc.completeRide(rideId, finalFare);
        return ResponseEntity.noContent().build();
    }
}
