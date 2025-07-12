/* ──────────────────────────────────────────────────────────────
 *  FILE : src/main/java/com/mobility/ride/service/DriverRideActionServiceImpl.java
 *  v2025-10-11 – accept SCHEDULED ≤ 25 min
 * ────────────────────────────────────────────────────────────── */
package com.mobility.ride.service;

import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;
import com.mobility.ride.repository.RideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional          // toutes les méthodes sont transactionnelles
public class DriverRideActionServiceImpl implements DriverRideActionService {

    private static final int ACCEPT_WINDOW_MIN = 25;       // ← fenêtre planifiées

    private final RideFlowService    rideFlow;
    private final RideLockingService locker;
    private final RideRepository     rideRepository;

    /* 1) ACCEPT / DECLINE -------------------------------------------------- */

    @Override
    public void accept(Long rideId, Long driverId) {
        locker.lockRide(rideId, () -> {

            /* ─── Contrôle “≤ 25 min” pour les planifiées ─── */
            Ride ride = rideRepository.findById(rideId).orElseThrow();
            if (ride.getStatus() == RideStatus.SCHEDULED) {
                OffsetDateTime now   = OffsetDateTime.now();
                OffsetDateTime gate  = now.plusMinutes(ACCEPT_WINDOW_MIN);
                if (ride.getScheduledAt().isAfter(gate)) {
                    throw new IllegalStateException(
                            "Trop tôt : vous pourrez accepter à partir de " +
                                    ride.getScheduledAt().minusMinutes(ACCEPT_WINDOW_MIN)
                                            .toLocalTime());
                }
            }

            /* ─── Transition métier ─── */
            rideFlow.accept(rideId, driverId);
        });
    }

    @Override
    public void decline(Long rideId, Long driverId) {
        locker.lockRide(rideId,
                () -> rideFlow.cancelRide(
                        rideId,
                        RideFlowService.CancellationReason.DRIVER));
    }

    /* 2) EN-ROUTE ➜ ARRIVED ➜ START --------------------------------------- */

    @Override public void startEnRoute(Long rideId) { locker.lockRide(
            rideId, () -> rideFlow.startEnRoute(rideId)); }

    @Override public void arrive(Long rideId) { locker.lockRide(
            rideId, () -> rideFlow.arrived(rideId)); }

    @Override public void startRide(Long rideId) { locker.lockRide(
            rideId, () -> rideFlow.startRide(rideId)); }

    /* 3) FINISH ------------------------------------------------------------ */

    @Override
    public void finish(Long rideId, double distanceKm, long durationSec) {
        locker.lockRide(rideId,
                () -> rideFlow.completeRide(rideId, distanceKm, durationSec));
    }
}
