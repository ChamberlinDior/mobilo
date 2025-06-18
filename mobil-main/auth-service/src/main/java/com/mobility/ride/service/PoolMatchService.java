package com.mobility.ride.service;

import com.mobility.ride.dto.PoolEligibilityRequest;
import com.mobility.ride.dto.PoolEligibilityResponse;
import com.mobility.ride.dto.PriceQuoteRequest;
import com.mobility.ride.dto.PriceQuoteResponse;
import com.mobility.ride.model.LatLng;
import com.mobility.ride.model.PoolGroup;
import com.mobility.ride.model.PoolGroupStatus;
import com.mobility.ride.model.ProductType;
import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;
import com.mobility.ride.repository.PoolGroupRepository;
import com.mobility.ride.repository.RideRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * <h2>Regroupement Pool & création de ride</h2>
 *
 * • Injection de CancellationPenaltyService afin de planifier le no-show.<br>
 * • Lorsqu’une course POOL est créée, on persiste l’entité Ride, on assigne au PoolGroup,
 *   puis on déclenche scheduleNoShowPenalty pour facturer si pas de prise en charge sous 2 min.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PoolMatchService {

    private final PoolGroupRepository           poolRepo;
    private final RideRepository                rideRepo;
    private final UpfrontPriceService           upfrontPriceService;
    private final CancellationPenaltyService    cancellationPenaltyService;

    /* ═══════════════════════ Public API ═══════════════════════ */

    /**
     * Vérifie qu’un utilisateur peut commander un Pool.
     */
    public PoolEligibilityResponse checkEligibility(PoolEligibilityRequest req) {
        boolean ok = req.productType() == ProductType.POOL
                && req.seatsRequested() <= 2;

        return PoolEligibilityResponse.builder()
                .eligible(ok)
                .reason(ok ? null : "NOT_ELIGIBLE_FOR_POOL")
                .build();
    }

    /**
     * Crée un « ride » Pool et renvoie le devis tarifaire up-front.
     * 1) Enregistre la course en base (Ride.status = REQUESTED par défaut).
     * 2) Associe à un PoolGroup existant ou nouveau.
     * 3) Planifie le no-show via scheduleNoShowPenalty (H+2 min).
     * 4) Calcule le prix fixe via UpfrontPriceService.
     */
    @Transactional
    public PriceQuoteResponse createPoolRide(PriceQuoteRequest req) {
        // 1) Création de l’entité Ride (statut REQUESTED par défaut)
        Ride ride = Ride.builder()
                .productType(req.productType())
                .status(RideStatus.REQUESTED)           // initialisation explicite
                .createdAt(OffsetDateTime.now())
                .build();
        rideRepo.save(ride);

        // 2) Assignation / création de PoolGroup
        joinOrCreatePool(ride, req.pickup(), req.dropoff());

        // 3) Planification du no-show (pénalité si non pris en charge sous 2 min)
        cancellationPenaltyService.scheduleNoShowPenalty(
                ride.getId(),
                CancellationPenaltyService.LATE_CANCEL_FEE,
                "XAF"
        );
        log.debug("⏳ No-show penalty scheduled for ride {}", ride.getId());

        // 4) Devis tarifaire up-front
        PriceQuoteResponse quote = upfrontPriceService.quote(req);

        log.info("🚗 Pool ride {} created (status={}) — upfront {} {}",
                ride.getId(), ride.getStatus(), quote.getTotalFare(), quote.getCurrency());

        return quote;
    }

    /* ═══════════════════════ Matching interne ═══════════════════════ */

    /**
     * Tente d’ajouter un ride à un groupe en formation.
     * Si aucun n’est compatible → création d’un nouveau PoolGroup.
     */
    @Transactional
    public PoolGroup joinOrCreatePool(Ride ride, LatLng pickup, LatLng drop) {
        // 1) Cherche un groupe FORMING dans un rayon de 500 m
        List<PoolGroup> candidates = poolRepo.findAllByStatus(PoolGroupStatus.FORMING);
        for (PoolGroup pg : candidates) {
            if (isCompatible(pg, pickup, drop)) {
                // verrou pessimiste pour assignation atomique
                PoolGroup locked = poolRepo.lockById(pg.getId())
                        .orElseThrow(() -> new EntityNotFoundException("POOL_LOCK_LOST"));
                locked.getRides().add(ride);
                ride.setPoolGroup(locked);
                rideRepo.save(ride);
                log.info("👥 Ride {} appended to PoolGroup {}", ride.getId(), locked.getId());
                return locked;
            }
        }

        // 2) Aucun groupe compatible → en créer un nouveau
        PoolGroup newGroup = PoolGroup.builder()
                .pickupLatLng(pickup)
                .dropLatLng(drop)
                .status(PoolGroupStatus.FORMING)
                .build();
        poolRepo.save(newGroup);

        ride.setPoolGroup(newGroup);
        rideRepo.save(ride);
        log.info("🆕 Ride {} started new PoolGroup {}", ride.getId(), newGroup.getId());
        return newGroup;
    }

    /* ═══════════════════════ Helpers ═══════════════════════ */

    /** Critère simplifié (et à affiner selon ETA, détour %, etc.). */
    private boolean isCompatible(PoolGroup pg, LatLng pickup, LatLng drop) {
        double maxDelta = 0.5; // km
        return distanceKm(pg.getPickupLatLng(), pickup) < maxDelta
                && distanceKm(pg.getDropLatLng(), drop) < maxDelta;
    }

    /** Haversine simplifié (précision suffisante < 1 km). */
    private double distanceKm(LatLng a, LatLng b) {
        double dLat = Math.toRadians(b.getLat() - a.getLat());
        double dLon = Math.toRadians(b.getLng() - a.getLng());
        double lat1 = Math.toRadians(a.getLat());
        double lat2 = Math.toRadians(b.getLat());

        double h = Math.pow(Math.sin(dLat / 2), 2)
                + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(lat1) * Math.cos(lat2);
        return 6371 * 2 * Math.asin(Math.sqrt(h)); // rayon Terre ≈ 6371 km
    }
}
