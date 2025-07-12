// ───────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/repository/RideRepository.java
//  v2025-10-10 – fenêtre open-ended pour les planifiées
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
 * Repository JPA pour {@link Ride}.
 */
@Repository
public interface RideRepository extends JpaRepository<Ride, Long> {

    /* ═════════════ 1) Recherches simples ═════════════ */
    List<Ride> findAllByRiderId(Long riderId);
    List<Ride> findAllByRiderIdOrderByCreatedAtDesc(Long riderId);
    List<Ride> findAllByDriverId(Long driverId);
    List<Ride> findAllByStatus(RideStatus status);
    @Override Optional<Ride> findById(Long id);

    /* ═════ 2) « Your Trips » – planifiées par rider ═════ */
    List<Ride> findByRiderIdAndStatusOrderByScheduledAtAsc(Long riderId, RideStatus status);
    List<Ride> findByRiderIdAndStatus(Long riderId, RideStatus status);

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

    /* ═════ 3) Fenêtres temporelles (bornées) ═════ */
    @Query("""
          select r from Ride r
           where r.status = com.mobility.ride.model.RideStatus.SCHEDULED
             and r.scheduledAt between :from and :to
          """)
    List<Ride> findScheduledBetween(@Param("from") OffsetDateTime from,
                                    @Param("to")   OffsetDateTime to);

    /* ═════ 3-bis) Fenêtre ouverte : >= now, sans limite haute ═════ */
    @Query("""
          select r from Ride r
           where r.status = com.mobility.ride.model.RideStatus.SCHEDULED
             and r.scheduledAt >= :from
          """)
    List<Ride> findScheduledAfter(@Param("from") OffsetDateTime from);

    /* ═════ 4) Historique rider / driver ═════ */
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

    /* ═════ 5) Driver offers – rides REQUESTED proches ═════ */
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
}
