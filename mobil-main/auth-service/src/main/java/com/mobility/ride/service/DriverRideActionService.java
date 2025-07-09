// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/DriverRideActionService.java
//  v2025-09-02 – API complète, alignée sur RideFlowService
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.service;

/**
 * Contrat exposé au contrôleur “Driver / Actions” afin de piloter
 * le flux temps-réel d’une course depuis l’application chauffeur.
 *
 * <p>Chaque méthode délègue la transition métier à {@link RideFlowService}
 * et est supposée verrouiller la ligne SQL via {@link RideLockingService}
 * pour éviter les conditions de course.</p>
 */
public interface DriverRideActionService {

    /* ═══════════ 1) ACCEPT / DECLINE ═══════════ */

    /**
     * Le chauffeur {@code driverId} accepte l’offre {@code rideId}.
     */
    void accept(Long rideId, Long driverId);

    /**
     * Le chauffeur {@code driverId} refuse l’offre {@code rideId}.
     */
    void decline(Long rideId, Long driverId);

    /* ═══════════ 2) EN-ROUTE ➜ ARRIVED ➜ START ═══════════ */

    /**
     * Le chauffeur démarre la navigation vers le point de prise en charge.
     */
    void startEnRoute(Long rideId);

    /**
     * Le chauffeur est arrivé au point de prise en charge
     * (déclenche le décompte d’attente / « grace period »).
     */
    void arrive(Long rideId);

    /**
     * Le passager est à bord ; la course passe à IN_PROGRESS.
     */
    void startRide(Long rideId);

    /* ═══════════ 3) FINISH (drop-off) ═══════════ */

    /**
     * Terminaison de la course.
     *
     * @param rideId       identifiant de la course
     * @param distanceKm   distance réelle parcourue (km)
     * @param durationSec  durée réelle (secondes)
     */
    void finish(Long rideId, double distanceKm, long durationSec);
}
