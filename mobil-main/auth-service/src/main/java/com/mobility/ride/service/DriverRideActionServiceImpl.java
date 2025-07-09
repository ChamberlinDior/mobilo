// ──────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/DriverRideActionServiceImpl.java
//  v2025-09-08 – transaction globale par méthode
// ──────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional      // ← toutes les méthodes sont maintenant transactionnelles
public class DriverRideActionServiceImpl implements DriverRideActionService {

    private final RideFlowService    rideFlow;
    private final RideLockingService locker;

    /* 1) ACCEPT / DECLINE -------------------------------------------------- */

    @Override
    public void accept(Long rideId, Long driverId) {
        locker.lockRide(rideId,
                () -> rideFlow.accept(rideId, driverId));
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
