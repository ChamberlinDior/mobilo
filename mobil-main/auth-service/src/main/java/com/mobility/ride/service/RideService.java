/* ─────────────────────────────────────────────────────────────
 *  FILE : src/main/java/com/mobility/ride/service/RideService.java
 *  v2025-10-07  « rider-photo-url-fix + crédit driver »
 *
 *     • Toutes les opérations CRUD + finishRide()
 *     • Crédit automatique du chauffeur via RideLifecycleService
 *     • Génération d’URL ou Base-64 inline pour la photo rider
 * ───────────────────────────────────────────────────────────── */
package com.mobility.ride.service;

import com.mobility.auth.model.User;
import com.mobility.auth.repository.UserRepository;
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
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RideService {

    /* ──────── Dépendances ──────── */
    private final RideRepository        rideRepository;
    private final RideLifecycleService  rideLifecycleService;  // + crédit driver
    private final PaymentService        paymentService;
    private final GeoService            geoService;
    private final UserRepository        userRepository;

    /* ═════════════════════ 1) DEMANDE IMMÉDIATE ═════════════════════ */
    @Transactional
    public RideResponse requestRide(RequestRideRequest req) {

        DeliveryZone zone   = Optional.ofNullable(req.getDeliveryZone())
                .orElse(DeliveryZone.LOCAL);
        BigDecimal   weight = Optional.ofNullable(req.getWeightKg())
                .orElse(BigDecimal.ZERO);

        Ride ride = Ride.builder()
                .riderId      (req.getRiderId())
                .pickupLat    (req.getPickupLat())
                .pickupLng    (req.getPickupLng())
                .dropoffLat   (req.getDropoffLat())
                .dropoffLng   (req.getDropoffLng())
                .productType  (com.mobility.ride.model.ProductType.valueOf(req.getProductType()))
                .options      (req.getOptions()==null ? null :
                        req.getOptions().stream()
                                .map(o -> com.mobility.ride.model.RideOption.valueOf(o))
                                .collect(Collectors.toList()))
                .totalFare    (req.getTotalFare())
                .currency     (req.getCurrency())
                .deliveryZone (zone)
                .weightKg     (weight)
                .status       (RideStatus.REQUESTED)
                .build();

        return toResponse(rideRepository.save(ride));
    }

    /* ═════════════════════ 2) PLANIFICATION ════════════════════════ */
    @Transactional
    public RideResponse scheduleRide(ScheduleRideRequest req) {

        DeliveryZone zone   = Optional.ofNullable(req.getDeliveryZone())
                .orElse(DeliveryZone.LOCAL);
        BigDecimal   weight = Optional.ofNullable(req.getWeightKg())
                .orElse(BigDecimal.ZERO);

        Ride ride = Ride.builder()
                .riderId        (req.getRiderId())
                .pickupLat      (req.getPickupLat())
                .pickupLng      (req.getPickupLng())
                .dropoffLat     (req.getDropoffLat())
                .dropoffLng     (req.getDropoffLng())
                .productType    (com.mobility.ride.model.ProductType.valueOf(req.getProductType()))
                .options        (req.getOptions()==null ? null :
                        req.getOptions().stream()
                                .map(o -> com.mobility.ride.model.RideOption.valueOf(o))
                                .collect(Collectors.toList()))
                .scheduledAt    (req.getScheduledAt())
                .paymentMethodId(req.getPaymentMethodId())
                .totalFare      (req.getTotalFare())
                .currency       (req.getCurrency())
                .deliveryZone   (zone)
                .weightKg       (weight)
                .status         (RideStatus.SCHEDULED)
                .build();

        Ride saved = rideRepository.save(ride);

        /* pré-autorisation paiement si nécessaire */
        try {
            paymentService.authorizeRide(saved, req.getTotalFare(), req.getCurrency());
        } catch (Exception ex) {
            System.err.printf("[PAYMENT] Pré-autorisation échouée (ride #%d) : %s%n",
                    saved.getId(), ex.getMessage());
        }

        return toResponse(saved);
    }

    /* ═════════════════════ 3) CONSULTATION ════════════════════════ */
    @Transactional(readOnly = true)
    public RideResponse getRide(Long rideId) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Course non trouvée – id=" + rideId));
        return toResponse(ride);
    }

    /* ═════════════════════ 4) LISTE PLANIFIÉES ════════════════════ */
    @Transactional(readOnly = true)
    public List<RideResponse> listScheduled(Long riderId) {
        return rideRepository.findByRiderIdAndStatus(riderId, RideStatus.SCHEDULED)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /* ═════════════════════ 5) HISTORIQUE ═════════════════════════ */
    @Transactional(readOnly = true)
    public List<RideResponse> listHistory(Long riderId) {
        return rideRepository.findAllByRiderId(riderId).stream()
                .sorted((a,b)->b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::toResponse)
                .toList();
    }

    /* ═════════════════════ 6) RE-PLANIFICATION ═══════════════════ */
    @Transactional
    public void reschedule(Long rideId, OffsetDateTime newTs) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Course non trouvée – id=" + rideId));

        if (ride.getStatus() != RideStatus.SCHEDULED)
            throw new IllegalStateException("Seules les courses SCHEDULED peuvent être re-planifiées");
        if (newTs.isBefore(OffsetDateTime.now().plusMinutes(1)))
            throw new IllegalArgumentException("La nouvelle date doit être ≥ 1 minute dans le futur");

        ride.setScheduledAt(newTs);
    }

    /* ═════════════════════ 7) FIN DE COURSE  ═════════════════════ */
    @Transactional
    public void finishRide(Long rideId, BigDecimal finalFare) {

        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Ride not found – id=" + rideId));

        ride.setStatus(RideStatus.COMPLETED);
        ride.setTotalFare(finalFare);

        /* Créditer immédiatement le chauffeur et historiser */
        rideLifecycleService.completeRide(rideId, finalFare);
    }

    /* ═════════════════════ 8) ENTITY ➜ DTO ══════════════════════ */
    private RideResponse toResponse(Ride r) {

        String pickupAddr  = geoService.reverse(r.getPickupLat(),  r.getPickupLng());
        String dropoffAddr = geoService.reverse(r.getDropoffLat(), r.getDropoffLng());

        /* Rider snippet */
        User rider = userRepository.findById(r.getRiderId()).orElse(null);

        String riderName = rider == null ? "—"
                : (Optional.ofNullable(rider.getFirstName()).orElse("") + " " +
                Optional.ofNullable(rider.getLastName()).orElse("")).trim();
        if (riderName.isBlank()) riderName = "—";

        /* Photo : URL ou Base-64 inline */
        String photoUrl = null;
        if (rider != null) {
            if (rider.getProfilePictureKey() != null) {
                photoUrl = "/api/v1/users/" + rider.getId() + "/photo";
            } else if (rider.getProfilePicture() != null) {
                String mime = Optional.ofNullable(rider.getProfilePictureMimeType())
                        .orElse("image/jpeg");
                photoUrl = "data:" + mime + ";base64," +
                        Base64.getEncoder().encodeToString(rider.getProfilePicture());
            }
        }

        return RideResponse.builder()
                .rideId         (r.getId())
                .status         (r.getStatus().name())
                .pickupLat      (r.getPickupLat())
                .pickupLng      (r.getPickupLng())
                .dropoffLat     (r.getDropoffLat())
                .dropoffLng     (r.getDropoffLng())
                .pickupAddress  (pickupAddr)
                .dropoffAddress (dropoffAddr)
                .productType    (r.getProductType().name())
                .options        (r.getOptions()==null ? null :
                        r.getOptions().stream().map(Enum::name).toList())
                .scheduledAt    (r.getScheduledAt())
                .paymentMethodId(r.getPaymentMethodId())
                .totalFare      (r.getTotalFare())
                .currency       (r.getCurrency())
                .weightKg       (r.getWeightKg())
                .deliveryZone   (r.getDeliveryZone().name())
                .safetyPin      (r.getSafetyPin())
                .createdAt      (r.getCreatedAt())
                /* champs utiles côté mobile */
                .riderName      (riderName)
                .riderPhone     (rider == null ? null : rider.getPhoneNumber())
                .riderPhotoUrl  (photoUrl)
                .build();
    }
}
