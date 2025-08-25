// ───────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/repository/RideRepository.java
//  v2025-10-12 – rider/driver active feeds + near offers + transitions atomiques
// ───────────────────────────────────────────────────────────
package com.mobility.ride.repository;

import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Repository JPA pour {@link Ride}.
 *
 * ⚙️ Points clés :
 * - Feeds “actifs” rider & driver (statuts non-terminaux).
 * - Fenêtres planifiées (rider & driver).
 * - Historique rider & driver.
 * - Offres proches (REQUESTED) pour les chauffeurs, pageable.
 * - Transitions atomiques courantes (assignation, en route, arrivé, à bord, cancel, complete).
 */
@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    /* ═════════════ 1) Recherches simples ═════════════ */
    List<Ride> findAllByRiderId(Long riderId);
    List<Ride> findAllByRiderIdOrderByCreatedAtDesc(Long riderId);
    List<Ride> findAllByDriverId(Long driverId);
    List<Ride> findAllByStatus(RideStatus status);
    @Override Optional<Ride> findById(Long id);

    /* ═════════════ 2) Feeds ACTIFS (rider & driver) ═════════════
       (REQUESTED, ACCEPTED, EN_ROUTE, ARRIVED, WAITING, IN_PROGRESS) */
    List<Ride> findByRiderIdAndStatusInOrderByCreatedAtDesc(
            Long riderId, Collection<RideStatus> statuses);

    List<Ride> findByDriverIdAndStatusInOrderByCreatedAtDesc(
            Long driverId, Collection<RideStatus> statuses);

    Optional<Ride> findTopByRiderIdAndStatusInOrderByCreatedAtDesc(
            Long riderId, Collection<RideStatus> statuses);

    Optional<Ride> findTopByDriverIdAndStatusInOrderByCreatedAtDesc(
            Long driverId, Collection<RideStatus> statuses);

    long countByRiderIdAndStatusIn(Long riderId, Collection<RideStatus> statuses);
    long countByDriverIdAndStatusIn(Long driverId, Collection<RideStatus> statuses);

    /* ═════ 3) « Your Trips » – planifiées (rider & driver) ═════ */
    List<Ride> findByRiderIdAndStatusOrderByScheduledAtAsc(Long riderId, RideStatus status);
    List<Ride> findByRiderIdAndStatus(Long riderId, RideStatus status);

    List<Ride> findByDriverIdAndStatusOrderByScheduledAtAsc(Long driverId, RideStatus status);
    List<Ride> findByDriverIdAndStatus(Long driverId, RideStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
          update Ride r
             set r.scheduledAt = :ts
           where r.id     = :id
             and r.status = com.mobility.ride.model.RideStatus.SCHEDULED
          """)
    int updateScheduledAt(@Param("id") Long id,
                          @Param("ts") OffsetDateTime ts);

    /* ═════ 4) Fenêtres temporelles planifiées ═════ */
    @Query("""
          select r from Ride r
           where r.status = com.mobility.ride.model.RideStatus.SCHEDULED
             and r.scheduledAt between :from and :to
          """)
    List<Ride> findScheduledBetween(@Param("from") OffsetDateTime from,
                                    @Param("to")   OffsetDateTime to);

    @Query("""
          select r from Ride r
           where r.status = com.mobility.ride.model.RideStatus.SCHEDULED
             and r.scheduledAt >= :from
          """)
    List<Ride> findScheduledAfter(@Param("from") OffsetDateTime from);

    /* ═════ 5) Historique rider / driver ═════ */
    @Query("""
          select r from Ride r
           where r.riderId = :riderId
             and r.createdAt between :from and :to
           order by r.createdAt desc
          """)
    List<Ride> findHistoryByRiderBetween(@Param("riderId") Long riderId,
                                         @Param("from")    OffsetDateTime from,
                                         @Param("to")      OffsetDateTime to);

    @Query("""
          select r from Ride r
           where r.driverId = :driverId
             and r.createdAt between :from and :to
           order by r.createdAt desc
          """)
    List<Ride> findHistoryByDriverBetween(@Param("driverId") Long driverId,
                                          @Param("from")     OffsetDateTime from,
                                          @Param("to")       OffsetDateTime to);

    /* ═════ 6) Driver offers – rides REQUESTED proches ═════
       NB: nécessite MySQL/MariaDB avec fonctions géo (ST_Distance_Sphere).
       Variante pageable pour trier par distance croissante. */
    @Query(value = """
          SELECT *
            FROM rides r
           WHERE r.status = 'REQUESTED'
             AND ST_Distance_Sphere(
                   POINT(r.pickup_lng, r.pickup_lat),
                   POINT(:lng, :lat)
                 ) < :radiusKm * 1000
          """,
            nativeQuery = true)
    List<Ride> findOpenNear(@Param("lat")      double lat,
                            @Param("lng")      double lng,
                            @Param("radiusKm") double radiusKm);

    @Query(value = """
          SELECT *
            FROM rides r
           WHERE r.status = 'REQUESTED'
             AND ST_Distance_Sphere(
                   POINT(r.pickup_lng, r.pickup_lat),
                   POINT(:lng, :lat)
                 ) < :radiusKm * 1000
           ORDER BY ST_Distance_Sphere(
                     POINT(r.pickup_lng, r.pickup_lat),
                     POINT(:lng, :lat)
                    ) ASC
          """,
            countQuery = """
          SELECT COUNT(*)
            FROM rides r
           WHERE r.status = 'REQUESTED'
             AND ST_Distance_Sphere(
                   POINT(r.pickup_lng, r.pickup_lat),
                   POINT(:lng, :lat)
                 ) < :radiusKm * 1000
          """,
            nativeQuery = true)
    Page<Ride> findOpenNearPage(@Param("lat") double lat,
                                @Param("lng") double lng,
                                @Param("radiusKm") double radiusKm,
                                Pageable pageable);

    /* ═════ 7) Transitions atomiques utiles (anti-concurrence) ═════
       ► Toutes renvoient le nombre de lignes mises à jour (0 = précondition non satisfaite). */

    /* 7.1 Assigner un chauffeur à une course encore REQUESTED */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
          update Ride r
             set r.driverId  = :driverId,
                 r.status    = com.mobility.ride.model.RideStatus.ACCEPTED,
                 r.acceptedAt = :ts
           where r.id        = :rideId
             and r.status    = com.mobility.ride.model.RideStatus.REQUESTED
             and r.driverId is null
          """)
    int assignDriverIfRequested(@Param("rideId") Long rideId,
                                @Param("driverId") Long driverId,
                                @Param("ts") OffsetDateTime acceptedAt);

    /* 7.2 Passer à EN_ROUTE si encore ACCEPTED et bien assigné */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
          update Ride r
             set r.status    = com.mobility.ride.model.RideStatus.EN_ROUTE,
                 r.enRouteAt = :ts
           where r.id        = :rideId
             and r.driverId  = :driverId
             and r.status    = com.mobility.ride.model.RideStatus.ACCEPTED
          """)
    int markEnRouteIfAccepted(@Param("rideId") Long rideId,
                              @Param("driverId") Long driverId,
                              @Param("ts") OffsetDateTime ts);

    /* 7.3 Passer à ARRIVED depuis EN_ROUTE (ou ACCEPTED selon le flux) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
          update Ride r
             set r.status    = com.mobility.ride.model.RideStatus.ARRIVED,
                 r.arrivedAt = :ts
           where r.id        = :rideId
             and r.driverId  = :driverId
             and r.status in (com.mobility.ride.model.RideStatus.EN_ROUTE,
                              com.mobility.ride.model.RideStatus.ACCEPTED)
          """)
    int markArrivedIfOnTheWay(@Param("rideId") Long rideId,
                              @Param("driverId") Long driverId,
                              @Param("ts") OffsetDateTime ts);

    /* 7.4 Passer à IN_PROGRESS si ARRIVED (prise en charge) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
          update Ride r
             set r.status       = com.mobility.ride.model.RideStatus.IN_PROGRESS,
                 r.pickupRealAt = :ts
           where r.id           = :rideId
             and r.driverId     = :driverId
             and r.status       = com.mobility.ride.model.RideStatus.ARRIVED
          """)
    int markInProgressIfArrived(@Param("rideId") Long rideId,
                                @Param("driverId") Long driverId,
                                @Param("ts") OffsetDateTime ts);

    /* 7.5 Annuler si pas encore terminé (pose CANCELLED + raison) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
          update Ride r
             set r.status       = com.mobility.ride.model.RideStatus.CANCELLED,
                 r.cancelledAt  = :ts,
                 r.cancelReason = :reason
           where r.id           = :rideId
             and r.status in (
                   com.mobility.ride.model.RideStatus.REQUESTED,
                   com.mobility.ride.model.RideStatus.ACCEPTED,
                   com.mobility.ride.model.RideStatus.EN_ROUTE,
                   com.mobility.ride.model.RideStatus.ARRIVED,
                   com.mobility.ride.model.RideStatus.WAITING,
                   com.mobility.ride.model.RideStatus.IN_PROGRESS,
                   com.mobility.ride.model.RideStatus.SCHEDULED
             )
          """)
    int cancelIfNotCompleted(@Param("rideId") Long rideId,
                             @Param("reason") String reason,
                             @Param("ts") OffsetDateTime ts);

    /* 7.6 Compléter si IN_PROGRESS (pose COMPLETED + dropoff + tarif final) */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
          update Ride r
             set r.status        = com.mobility.ride.model.RideStatus.COMPLETED,
                 r.dropoffRealAt = :ts,
                 r.totalFare     = :fare
           where r.id            = :rideId
             and r.driverId      = :driverId
             and r.status        = com.mobility.ride.model.RideStatus.IN_PROGRESS
          """)
    int completeIfInProgress(@Param("rideId") Long rideId,
                             @Param("driverId") Long driverId,
                             @Param("ts") OffsetDateTime ts,
                             @Param("fare") BigDecimal finalFare);
}
