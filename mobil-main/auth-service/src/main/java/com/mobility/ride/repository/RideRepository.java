// ───────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/repository/RideRepository.java
//  v2025-06-15 – « Your Trips / History & Re-planification »
// ───────────────────────────────────────────────────────────
package com.mobility.ride.repository;

import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA de l’entité {@link Ride}.
 *
 * <p>Fonctionnalités :</p>
 * <ul>
 *   <li>CRUD et recherches Spring-Data ;</li>
 *   <li>Listing « À venir » (SCHEDULED) trié sur <code>scheduledAt</code> ;</li>
 *   <li>Historique complet trié sur <code>createdAt</code> (desc) ;</li>
 *   <li>Re-planification transactionnelle (<code>updateScheduledAt</code>) ;</li>
 *   <li>Fenêtrage temporel (between) pour l’analytics ou le back-office.</li>
 * </ul>
 */
@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    /* ═══════════════════════════════════════════════════════
       1) Recherches simples
       ═══════════════════════════════════════════════════════ */

    /** Tous les rides d’un passager, sans ordre. */
    List<Ride> findAllByRiderId(Long riderId);

    /** Historique complet, newest → oldest (pour l’onglet « Historique »). */
    List<Ride> findAllByRiderIdOrderByCreatedAtDesc(Long riderId);

    /** Rides d’un conducteur. */
    List<Ride> findAllByDriverId(Long driverId);

    /** Tous les rides d’un statut donné. */
    List<Ride> findAllByStatus(RideStatus status);

    /** Lookup direct (hérité mais exposé pour clarté). */
    @Override
    Optional<Ride> findById(Long id);

    /* ═══════════════════════════════════════════════════════
       2) « Your Trips » – listing planifié
       ═══════════════════════════════════════════════════════ */

    /**
     * Rides à venir d’un passager, triés du plus proche au plus lointain.
     */
    List<Ride> findByRiderIdAndStatusOrderByScheduledAtAsc(
            Long riderId,
            RideStatus status  /* = SCHEDULED */
    );

    /** Variante non triée (conservation API existante). */
    List<Ride> findByRiderIdAndStatus(Long riderId, RideStatus status);

    /**
     * Mise à jour atomique de la date/heure planifiée.
     *
     * @return le nombre de lignes impactées (0 ou 1 normalement).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
        update Ride r
           set r.scheduledAt = :ts
         where r.id         = :id
           and r.status     = com.mobility.ride.model.RideStatus.SCHEDULED
        """)
    int updateScheduledAt(
            @Param("id") Long id,
            @Param("ts") OffsetDateTime ts
    );

    /* ═══════════════════════════════════════════════════════
       3) Fenêtres temporelles  (analytics / exports)
       ═══════════════════════════════════════════════════════ */

    /** Tous les rides planifiés entre deux dates (par ex. cron de rappel). */
    @Query("""
        select r from Ride r
         where r.status = com.mobility.ride.model.RideStatus.SCHEDULED
           and r.scheduledAt between :from and :to
        """)
    List<Ride> findScheduledBetween(
            @Param("from") OffsetDateTime from,
            @Param("to")   OffsetDateTime to
    );

    /** Historique d’un passager sur une période. */
    @Query("""
        select r from Ride r
         where r.riderId   = :riderId
           and r.createdAt between :from and :to
         order by r.createdAt desc
        """)
    List<Ride> findHistoryByRiderBetween(
            @Param("riderId") Long riderId,
            @Param("from")    OffsetDateTime from,
            @Param("to")      OffsetDateTime to
    );

    /** Historique d’un conducteur sur une période. */
    @Query("""
        select r from Ride r
         where r.driverId  = :driverId
           and r.createdAt between :from and :to
         order by r.createdAt desc
        """)
    List<Ride> findHistoryByDriverBetween(
            @Param("driverId") Long driverId,
            @Param("from")     OffsetDateTime from,
            @Param("to")       OffsetDateTime to
    );
}
