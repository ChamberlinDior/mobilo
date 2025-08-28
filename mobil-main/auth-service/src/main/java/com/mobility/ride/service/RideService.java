// ─────────────────────────────────────────────────────────────
// FILE : src/main/java/com/mobility/ride/service/RideService.java
// v2025-10-13  – affichage fiable (rider/driver/colis), préfetch Users,
//                feeds actifs, offres proches, transitions driver,
//                alias listScheduled/listHistory, +completedAt & tri HISTORY.
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.auth.model.User;
import com.mobility.auth.repository.UserRepository;
import com.mobility.ride.dto.RequestRideRequest;
import com.mobility.ride.dto.RideResponse;
import com.mobility.ride.dto.ScheduleRideRequest;
import com.mobility.ride.model.DeliveryZone;
import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideOption;
import com.mobility.ride.model.RideStatus;
import com.mobility.ride.repository.RideRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class RideService {

    /* ──────── Dépendances ──────── */
    private final RideRepository       rideRepository;
    private final RideLifecycleService rideLifecycleService;  // paiement + wallet
    private final PaymentService       paymentService;
    private final GeoService           geoService;
    private final UserRepository       userRepository;

    /* ──────── Jeux d’états utiles ──────── */
    private static final Set<RideStatus> ACTIVE_STATUSES = EnumSet.of(
            RideStatus.REQUESTED, RideStatus.ACCEPTED, RideStatus.EN_ROUTE,
            RideStatus.ARRIVED, RideStatus.WAITING, RideStatus.IN_PROGRESS
    );

    /* ═════════════════════ 1) DEMANDE IMMÉDIATE ═════════════════════ */
    @Transactional
    public RideResponse requestRide(RequestRideRequest req) {
        DeliveryZone zone   = Optional.ofNullable(req.getDeliveryZone()).orElse(DeliveryZone.LOCAL);
        BigDecimal   weight = Optional.ofNullable(req.getWeightKg()).orElse(BigDecimal.ZERO);

        Ride ride = Ride.builder()
                .riderId      (req.getRiderId())
                .pickupLat    (req.getPickupLat())
                .pickupLng    (req.getPickupLng())
                .dropoffLat   (req.getDropoffLat())
                .dropoffLng   (req.getDropoffLng())
                .productType  (com.mobility.ride.model.ProductType.valueOf(req.getProductType()))
                .options      (safeOptions(req.getOptions()))
                .totalFare    (req.getTotalFare())
                .currency     (req.getCurrency())
                .deliveryZone (zone)
                .weightKg     (weight)
                .status       (RideStatus.REQUESTED)
                .build();

        Ride saved = rideRepository.save(ride);
        return toResponseSingle(saved);
    }

    /* ═════════════════════ 2) PLANIFICATION ════════════════════════ */
    @Transactional
    public RideResponse scheduleRide(ScheduleRideRequest req) {
        DeliveryZone zone   = Optional.ofNullable(req.getDeliveryZone()).orElse(DeliveryZone.LOCAL);
        BigDecimal   weight = Optional.ofNullable(req.getWeightKg()).orElse(BigDecimal.ZERO);

        Ride ride = Ride.builder()
                .riderId        (req.getRiderId())
                .pickupLat      (req.getPickupLat())
                .pickupLng      (req.getPickupLng())
                .dropoffLat     (req.getDropoffLat())
                .dropoffLng     (req.getDropoffLng())
                .productType    (com.mobility.ride.model.ProductType.valueOf(req.getProductType()))
                .options        (safeOptions(req.getOptions()))
                .scheduledAt    (req.getScheduledAt())
                .paymentMethodId(req.getPaymentMethodId())
                .totalFare      (req.getTotalFare())
                .currency       (req.getCurrency())
                .deliveryZone   (zone)
                .weightKg       (weight)
                .status         (RideStatus.SCHEDULED)
                .build();

        Ride saved = rideRepository.save(ride);

        // Pré-autorisation non bloquante
        try {
            paymentService.authorizeRide(saved, req.getTotalFare(), req.getCurrency());
        } catch (Exception ex) {
            System.err.printf("[PAYMENT] Pré-autorisation échouée (ride #%d) : %s%n",
                    saved.getId(), ex.getMessage());
        }

        return toResponseSingle(saved);
    }

    /* ═════════════════════ 3) CONSULTATION (read) ═══════════════════ */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public RideResponse getRide(Long rideId) {
        Ride r = rideRepository.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("Course non trouvée – id=" + rideId));
        return toResponseSingle(r);
    }

    /* ═════════════════════ 4) FEEDS ACTIFS (rider/driver) ══════════ */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<RideResponse> listActiveForRider(Long riderId) {
        List<Ride> rides = rideRepository
                .findByRiderIdAndStatusInOrderByCreatedAtDesc(riderId, ACTIVE_STATUSES);
        return mapWithPrefetch(rides);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<RideResponse> currentForRider(Long riderId) {
        return rideRepository
                .findTopByRiderIdAndStatusInOrderByCreatedAtDesc(riderId, ACTIVE_STATUSES)
                .map(this::toResponseSingle);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<RideResponse> listActiveForDriver(Long driverId) {
        List<Ride> rides = rideRepository
                .findByDriverIdAndStatusInOrderByCreatedAtDesc(driverId, ACTIVE_STATUSES);
        return mapWithPrefetch(rides);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public Optional<RideResponse> currentForDriver(Long driverId) {
        return rideRepository
                .findTopByDriverIdAndStatusInOrderByCreatedAtDesc(driverId, ACTIVE_STATUSES)
                .map(this::toResponseSingle);
    }

    /* ═════════════════════ 5) LISTES PLANIFIÉES ═══════════════════ */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<RideResponse> listScheduledForRider(Long riderId) {
        List<Ride> rides = rideRepository
                .findByRiderIdAndStatusOrderByScheduledAtAsc(riderId, RideStatus.SCHEDULED);
        return mapWithPrefetch(rides);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<RideResponse> listScheduledForDriver(Long driverId) {
        List<Ride> rides = rideRepository
                .findByDriverIdAndStatusOrderByScheduledAtAsc(driverId, RideStatus.SCHEDULED);
        return mapWithPrefetch(rides);
    }

    /* ═════════════════════ 6) HISTORIQUE (rider/driver) ═══════════ */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<RideResponse> listHistoryForRider(Long riderId) {
        var rides = rideRepository.findAllByRiderIdOrderByCreatedAtDesc(riderId).stream()
                .filter(r -> r.getStatus() == RideStatus.COMPLETED || r.getStatus() == RideStatus.CANCELLED)
                .sorted(Comparator.comparing(
                        (Ride r) -> Optional.ofNullable(r.getDropoffRealAt()).orElse(r.getCreatedAt())
                ).reversed())
                .toList();
        return mapWithPrefetch(rides);
    }

    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<RideResponse> listHistoryForDriver(Long driverId) {
        var rides = rideRepository.findAllByDriverId(driverId).stream()
                .filter(r -> r.getStatus() == RideStatus.COMPLETED || r.getStatus() == RideStatus.CANCELLED)
                .sorted(Comparator.comparing(
                        (Ride r) -> Optional.ofNullable(r.getDropoffRealAt()).orElse(r.getCreatedAt())
                ).reversed())
                .toList();
        return mapWithPrefetch(rides);
    }

    /* ════ ALIAS (compatibilité anciens contrôleurs) ════ */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<RideResponse> listScheduled(Long riderId) {     // ← alias
        return listScheduledForRider(riderId);
    }
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<RideResponse> listHistory(Long riderId) {       // ← alias
        return listHistoryForRider(riderId);
    }

    /* ═════════════════════ 7) RE-PLANIFICATION ═══════════════════ */
    @Transactional
    public void reschedule(Long rideId, OffsetDateTime newTs) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("Course non trouvée – id=" + rideId));

        if (ride.getStatus() != RideStatus.SCHEDULED)
            throw new IllegalStateException("Seules les courses SCHEDULED peuvent être re-planifiées");
        if (newTs.isBefore(OffsetDateTime.now().plusMinutes(1)))
            throw new IllegalArgumentException("La nouvelle date doit être ≥ 1 minute dans le futur");

        ride.setScheduledAt(newTs);
    }

    /* ═════════════════════ 8) OFFRES PROCHES (driver) ═════════════ */
    @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
    public List<RideResponse> listOpenNear(double lat, double lng, double radiusKm) {
        List<Ride> rides = rideRepository.findOpenNear(lat, lng, radiusKm).stream()
                .sorted(Comparator.comparing(Ride::getCreatedAt).reversed())
                .toList();
        return mapWithPrefetch(rides);
    }

    /* ═════════════════════ 9) TRANSITIONS DRIVER (atomiques) ══════ */
    @Transactional
    public boolean assignIfAvailable(Long rideId, Long driverId) {
        int updated = rideRepository.assignDriverIfRequested(rideId, driverId, OffsetDateTime.now());
        return updated > 0;
    }

    @Transactional
    public boolean markEnRoute(Long rideId, Long driverId) {
        int updated = rideRepository.markEnRouteIfAccepted(rideId, driverId, OffsetDateTime.now());
        return updated > 0;
    }

    @Transactional
    public boolean markArrived(Long rideId, Long driverId) {
        int updated = rideRepository.markArrivedIfOnTheWay(rideId, driverId, OffsetDateTime.now());
        return updated > 0;
    }

    @Transactional
    public boolean startRide(Long rideId, Long driverId) {
        int updated = rideRepository.markInProgressIfArrived(rideId, driverId, OffsetDateTime.now());
        return updated > 0;
    }

    @Transactional
    public boolean cancelRide(Long rideId, String reason) {
        int updated = rideRepository.cancelIfNotCompleted(rideId, reason, OffsetDateTime.now());
        return updated > 0;
    }

    /* ═════════════════════ 10) FIN DE COURSE ═════════════════════ */
    @Transactional
    public void driverComplete(Long rideId, Long driverId, BigDecimal finalFare) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("Ride not found – id=" + rideId));

        if (!Objects.equals(ride.getDriverId(), driverId))
            throw new IllegalStateException("Driver non autorisé pour cette course");
        if (ride.getStatus() != RideStatus.IN_PROGRESS)
            throw new IllegalStateException("La course doit être IN_PROGRESS pour être complétée");

        ride.setDropoffRealAt(OffsetDateTime.now());
        rideLifecycleService.completeRide(rideId, finalFare);
    }

    /* Legacy (rétro-compat) */
    @Transactional
    public void finishRide(Long rideId, BigDecimal finalFare) {
        Ride ride = rideRepository.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("Ride not found – id=" + rideId));
        if (ride.getStatus() != RideStatus.IN_PROGRESS)
            throw new IllegalStateException("Ride must be IN_PROGRESS to complete");
        ride.setDropoffRealAt(OffsetDateTime.now());
        rideLifecycleService.completeRide(rideId, finalFare);
    }

    /* ═════════════════════ 11) MAPPING ENTITY ➜ DTO ═══════════════ */

    /** Mapping optimisé pour une entité unique (précharge users via findAllById). */
    private RideResponse toResponseSingle(Ride r) {
        Set<Long> ids = Stream.of(r.getRiderId(), r.getDriverId())
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> users = ids.isEmpty() ? Collections.emptyMap()
                : userRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return toResponse(r, users);
    }

    /** Mapping optimisé pour une liste (précharge tous les users en 1 fois). */
    private List<RideResponse> mapWithPrefetch(List<Ride> rides) {
        Set<Long> userIds = rides.stream()
                .flatMap(r -> Stream.of(r.getRiderId(), r.getDriverId()))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap()
                : userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        return rides.stream().map(r -> toResponse(r, users)).toList();
    }

    private RideResponse toResponse(Ride r, Map<Long, User> usersById) {
        // Géocodage (fallback assuré par GeoServiceImpl)
        String pickupAddr  = geoService.reverse(r.getPickupLat(),  r.getPickupLng());
        String dropoffAddr = geoService.reverse(r.getDropoffLat(), r.getDropoffLng());

        // Rider / Driver
        User rider  = (r.getRiderId()  == null) ? null : usersById.get(r.getRiderId());
        User driver = (r.getDriverId() == null) ? null : usersById.get(r.getDriverId());

        String riderName   = displayName(rider);
        String riderPhone  = rider  == null ? null : rider.getPhoneNumber();
        String riderPhoto  = photoUrl(rider);

        String driverName  = displayName(driver);
        String driverPhone = driver == null ? null : driver.getPhoneNumber();
        String driverPhoto = photoUrl(driver);

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
                .options        (r.getOptions()==null ? null : r.getOptions().stream().map(Enum::name).toList())
                .scheduledAt    (r.getScheduledAt())
                .paymentMethodId(r.getPaymentMethodId())
                .totalFare      (r.getTotalFare())
                .currency       (r.getCurrency())
                .weightKg       (r.getWeightKg())
                .deliveryZone   (r.getDeliveryZone()==null ? null : r.getDeliveryZone().name())
                .safetyPin      (r.getSafetyPin())
                .createdAt      (r.getCreatedAt())
                .completedAt    (r.getDropoffRealAt())      // ✅ clé pour l’onglet HISTORY

                // Vue chauffeur (passager)
                .riderName      (riderName)
                .riderPhone     (riderPhone)
                .riderPhotoUrl  (riderPhoto)

                // Vue passager (chauffeur)
                .driverName     (driverName)
                .driverPhone    (driverPhone)
                .driverPhotoUrl (driverPhoto)
                .build();
    }

    /* ───────────────── Helpers ───────────────── */

    private List<RideOption> safeOptions(List<String> raw) {
        if (raw == null) return null;
        List<RideOption> list = new ArrayList<>();
        for (String s : raw) {
            if (s == null) continue;
            try {
                list.add(RideOption.valueOf(s));
            } catch (IllegalArgumentException ignore) { /* option inconnue → on ignore */ }
        }
        return list.isEmpty() ? null : list;
    }

    private String displayName(User u) {
        if (u == null) return "—";
        String n = (Optional.ofNullable(u.getFirstName()).orElse("") + " " +
                Optional.ofNullable(u.getLastName()).orElse("")).trim();
        return n.isBlank() ? "—" : n;
    }

    private String photoUrl(User u) {
        if (u == null) return null;
        if (u.getProfilePictureKey() != null) {
            return "/api/v1/users/" + u.getId() + "/photo";
        } else if (u.getProfilePicture() != null) {
            String mime = Optional.ofNullable(u.getProfilePictureMimeType()).orElse("image/jpeg");
            return "data:" + mime + ";base64," +
                    java.util.Base64.getEncoder().encodeToString(u.getProfilePicture());
        }
        return null;
    }
}
