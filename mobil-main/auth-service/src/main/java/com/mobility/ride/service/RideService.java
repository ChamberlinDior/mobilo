// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/RideService.java
//  v2025-06-15 – « Your Trips / History & Re-planification »
//  + mises à jour 2025-07-01 pour livraisons interurbain & international
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.dto.RequestRideRequest;
import com.mobility.ride.dto.RideResponse;
import com.mobility.ride.dto.ScheduleRideRequest;
import com.mobility.ride.model.DeliveryZone;
import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;
import com.mobility.ride.repository.RideRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideService {

    private final RideRepository  rideRepository;
    private final PaymentService   paymentService;
    private final GeoService       geoService;       // ★ reverse‐geocoding

    /* ═══════════════════════════════════════════════════════════
       1) DEMANDE IMMÉDIATE
       ═══════════════════════════════════════════════════════════ */
    @Transactional
    public RideResponse requestRide(RequestRideRequest req) {
        // 1) valeur par défaut pour courses “classiques”
        DeliveryZone zone   = Optional.ofNullable(req.getDeliveryZone())
                .orElse(DeliveryZone.LOCAL);
        BigDecimal    weight = Optional.ofNullable(req.getWeightKg())
                .orElse(BigDecimal.ZERO);

        Ride ride = Ride.builder()
                .riderId(req.getRiderId())
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .dropoffLat(req.getDropoffLat())
                .dropoffLng(req.getDropoffLng())
                .productType(com.mobility.ride.model.ProductType.valueOf(req.getProductType()))
                .options(req.getOptions() == null ? null
                        : req.getOptions().stream()
                        .map(o -> com.mobility.ride.model.RideOption.valueOf(o))
                        .collect(Collectors.toList()))
                // champs tarifaires & livraison
                .totalFare(req.getTotalFare())
                .currency(req.getCurrency())
                // on enregistre toujours une zone
                .deliveryZone(zone)
                // on stocke 0 kg pour LOCAL, ou le poids fourni sinon
                .weightKg(weight)
                // createdAt / safetyPin / status initial sont gérés par @PrePersist
                .build();

        Ride saved = rideRepository.save(ride);
        return toResponse(saved);
    }

    /* ═══════════════════════════════════════════════════════════
       2) PLANIFICATION FUTURE
       ═══════════════════════════════════════════════════════════ */
    @Transactional
    public RideResponse scheduleRide(ScheduleRideRequest req) {
        // mêmes valeurs par défaut qu’au-dessus
        DeliveryZone zone   = Optional.ofNullable(req.getDeliveryZone())
                .orElse(DeliveryZone.LOCAL);
        BigDecimal    weight = Optional.ofNullable(req.getWeightKg())
                .orElse(BigDecimal.ZERO);

        Ride ride = Ride.builder()
                .riderId(req.getRiderId())
                .pickupLat(req.getPickupLat())
                .pickupLng(req.getPickupLng())
                .dropoffLat(req.getDropoffLat())
                .dropoffLng(req.getDropoffLng())
                .productType(com.mobility.ride.model.ProductType.valueOf(req.getProductType()))
                .options(req.getOptions() == null ? null
                        : req.getOptions().stream()
                        .map(o -> com.mobility.ride.model.RideOption.valueOf(o))
                        .collect(Collectors.toList()))
                .scheduledAt(req.getScheduledAt())
                .paymentMethodId(req.getPaymentMethodId())
                .totalFare(req.getTotalFare())
                .currency(req.getCurrency())
                // on enregistre toujours une zone
                .deliveryZone(zone)
                // on stocke 0 kg pour LOCAL, ou le poids fourni sinon
                .weightKg(weight)
                .status(RideStatus.SCHEDULED)
                .build();

        Ride saved = rideRepository.save(ride);

        // Pré-autorisation (stub en profil « local »)
        try {
            paymentService.authorizeRide(saved, req.getTotalFare(), req.getCurrency());
        } catch (Exception ex) {
            System.err.printf("[PAYMENT] Pré-autorisation échouée (ride #%d) : %s%n",
                    saved.getId(), ex.getMessage());
        }

        return toResponse(saved);
    }

    /* ═══════════════════════════════════════════════════════════
       3) LECTURE INDIVIDUELLE
       ═══════════════════════════════════════════════════════════ */
    @Transactional(readOnly = true)
    public RideResponse getRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Course non trouvée – id=" + rideId));
        return toResponse(ride);
    }

    /* ═══════════════════════════════════════════════════════════
       4) LISTE – RIDES PLANIFIÉS (« À venir »)
       ═══════════════════════════════════════════════════════════ */
    @Transactional(readOnly = true)
    public List<RideResponse> listScheduled(Long riderId) {
        return rideRepository.findByRiderIdAndStatus(riderId, RideStatus.SCHEDULED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* ═══════════════════════════════════════════════════════════
       5) LISTE – HISTORIQUE COMPLET
       ═══════════════════════════════════════════════════════════ */
    @Transactional(readOnly = true)
    public List<RideResponse> listHistory(Long riderId) {
        return rideRepository.findAllByRiderId(riderId).stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    /* ═══════════════════════════════════════════════════════════
       6) RE-PLANIFICATION
       ═══════════════════════════════════════════════════════════ */
    @Transactional
    public void reschedule(Long rideId, OffsetDateTime newTs) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Course non trouvée – id=" + rideId));

        if (ride.getStatus() != RideStatus.SCHEDULED) {
            throw new IllegalStateException(
                    "Seules les courses SCHEDULED peuvent être re-planifiées");
        }
        if (newTs.isBefore(OffsetDateTime.now().plusMinutes(1))) {
            throw new IllegalArgumentException(
                    "La nouvelle date doit être ≥ 1 minute dans le futur");
        }

        ride.setScheduledAt(newTs);
    }

    /* ═══════════════════════════════════════════════════════════
       7) Mapping ENTITY → DTO
       ═══════════════════════════════════════════════════════════ */
    private RideResponse toResponse(Ride r) {
        String puAddr = geoService.reverse(r.getPickupLat(), r.getPickupLng());
        String doAddr = geoService.reverse(r.getDropoffLat(), r.getDropoffLng());

        return RideResponse.builder()
                .rideId(r.getId())
                .status(r.getStatus().name())
                .pickupLat(r.getPickupLat())
                .pickupLng(r.getPickupLng())
                .dropoffLat(r.getDropoffLat())
                .dropoffLng(r.getDropoffLng())
                .pickupAddress(puAddr)
                .dropoffAddress(doAddr)
                .productType(r.getProductType().name())
                .options(r.getOptions() == null ? null
                        : r.getOptions().stream().map(Enum::name).toList())
                .scheduledAt(r.getScheduledAt())
                .paymentMethodId(r.getPaymentMethodId())
                .totalFare(r.getTotalFare())
                .currency(r.getCurrency())
                .weightKg(r.getWeightKg())
                .deliveryZone(r.getDeliveryZone().name())
                .safetyPin(r.getSafetyPin())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
