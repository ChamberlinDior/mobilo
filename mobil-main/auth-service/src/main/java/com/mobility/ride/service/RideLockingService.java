// ──────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/RideLockingService.java
//  v2025-09-08 – lock & execute (PESSIMISTIC_WRITE)
// ──────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.repository.RideRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PessimisticLockException;
import jakarta.persistence.QueryTimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RideLockingService {

    private static final int LOCK_TIMEOUT_SEC =
            (int) Duration.ofSeconds(5).toSeconds();

    private final EntityManager em;
    private final RideRepository rideRepo;

    /** Verrouille la ligne *rides* puis exécute la logique métier. */
    @Transactional(propagation = Propagation.MANDATORY)
    public <T> T lockRide(Long rideId, Supplier<T> body) {

        // 1) Timeout MySQL : 5 s
        em.unwrap(Session.class).doWork(conn -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SET SESSION innodb_lock_wait_timeout = ?")) {
                ps.setInt(1, LOCK_TIMEOUT_SEC);
                ps.execute();
            }
        });

        try {
            // 2) Verrou PESSIMISTIC_WRITE
            em.lock(rideRepo.getReferenceById(rideId),
                    LockModeType.PESSIMISTIC_WRITE);

            // 3) Logique métier atomique
            return body.get();

        } catch (PessimisticLockException | QueryTimeoutException ex) {
            log.warn("🔒 Ride {} – lock failed: {}", rideId,
                    ex.getClass().getSimpleName());
            throw ex;
        }
    }

    /** Surcharge pratique pour les lambdas `void`. */
    public void lockRide(Long rideId, Runnable r) {
        lockRide(rideId, () -> { r.run(); return null; });
    }
}
