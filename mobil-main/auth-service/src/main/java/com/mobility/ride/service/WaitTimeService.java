// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/WaitTimeService.java
//  v2025-09-03 – @Lazy RideFlowService  +  @Qualifier("taskScheduler")
//                pour éliminer le cycle RideFlowService ↔ WaitTimeService
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;
import com.mobility.ride.repository.RideRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * <h2>WaitTimeService</h2>
 *
 * <p>
 *  • Passe automatiquement une course <strong>ARRIVED → WAITING</strong>
 *    après la période de grâce (2 min).<br>
 *  • Incrémente toutes les 30 s :
 *      <ul>
 *        <li>{@code waitingSec}</li>
 *        <li>{@code waitFee} (arrondi 0,01)</li>
 *      </ul>
 *  • Déclenche un <em>no-show</em> + frais forfaitaire après 7 min.<br>
 *  • Stoppe immédiatement le scheduler si la course sort du flux d’attente.
 * </p>
 *
 * <p>
 * <b>Important :</b> on injecte {@link RideFlowService} en <em>lazy</em> et le
 * {@link TaskScheduler} via <code>@Qualifier("taskScheduler")</code> pour
 * éviter tout cycle de dépendances et interférence avec le scheduler STOMP.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WaitTimeService {

    /* ─────── Paramètres métier (constants) ─────── */
    public static final Duration   GRACE_PERIOD   = Duration.ofMinutes(2);
    public static final Duration   TICK_INTERVAL  = Duration.ofSeconds(30);
    public static final Duration   NO_SHOW_LIMIT  = Duration.ofMinutes(7);
    public static final BigDecimal FEE_PER_MIN    = new BigDecimal("0.30");   // 0,30 €/min
    public static final BigDecimal NO_SHOW_FEE    = new BigDecimal("3.00");   // forfait

    /* ─────── Dépendances ─────── */
    private final @Qualifier("taskScheduler") TaskScheduler scheduler; // ✅ scheduler dédié
    private final RideRepository               rideRepo;
    private final @Lazy RideFlowService        flowSvc;                // ✅ élimine le cycle

    /** Tâches planifiées par rideId (pour annulation propre). */
    private final Map<Long, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    /* ═══════════════════════════════════════════════════════
       1) startWaitingCountdown – appelé par RideFlow.arrived()
       ═══════════════════════════════════════════════════════ */
    public void startWaitingCountdown(Long rideId) {

        // Évite de programmer deux fois la même course
        if (tasks.containsKey(rideId)) return;

        ScheduledFuture<?> f = scheduler.scheduleAtFixedRate(
                new Tick(rideId),
                OffsetDateTime.now().plus(GRACE_PERIOD).toInstant(),
                TICK_INTERVAL);

        tasks.put(rideId, f);

        log.debug("⏳ WaitTimeService – ride #{} scheduled (grace {}, tick {})",
                rideId, GRACE_PERIOD, TICK_INTERVAL);
    }

    /* ═══════════════════════════════════════════════════════
       2) stopWaitingCountdown – appelé par RideFlow.startRide()
       ═══════════════════════════════════════════════════════ */
    public void stopWaitingCountdown(Long rideId) {
        Optional.ofNullable(tasks.remove(rideId)).ifPresent(f -> f.cancel(false));
        log.debug("✅ WaitTimeService – ride #{} scheduler stopped", rideId);
    }

    /* ═══════════════════════════════════════════════════════
       3) Tâche périodique interne
       ═══════════════════════════════════════════════════════ */
    private class Tick implements Runnable {

        private final Long rideId;

        private Tick(Long rideId) { this.rideId = rideId; }

        @Override
        @Transactional
        public void run() {
            Ride r = rideRepo.findById(rideId)
                    .orElseThrow(() -> new EntityNotFoundException("RIDE_NOT_FOUND"));

            switch (r.getStatus()) {

                /* ARRIVED → première exécution post-grâce ⇒ passage à WAITING */
                case ARRIVED -> {
                    r.setStatus(RideStatus.WAITING);
                    r.setWaitingSec(0);
                    r.setWaitFee(BigDecimal.ZERO);
                    rideRepo.save(r);
                    log.info("🕒 Ride #{} entered WAITING (fee starts now)", rideId);
                }

                /* WAITING → incrémente temps & frais, puis check NO_SHOW */
                case WAITING -> {
                    int sec = Optional.ofNullable(r.getWaitingSec()).orElse(0)
                            + (int) TICK_INTERVAL.getSeconds();
                    r.setWaitingSec(sec);

                    int minutes   = (sec + 59) / 60;   // arrondi à la minute supérieure
                    BigDecimal fee = FEE_PER_MIN
                            .multiply(BigDecimal.valueOf(minutes))
                            .setScale(2, RoundingMode.HALF_UP);
                    r.setWaitFee(fee);

                    rideRepo.save(r);
                    log.debug("⌛ Ride #{} waiting {} s → fee {}", rideId, sec, fee);

                    /* No-show automatique ? */
                    if (sec >= NO_SHOW_LIMIT.toSeconds()) {
                        log.warn("🚫 Ride #{} no-show – auto-cancel & fee {}", rideId, NO_SHOW_FEE);
                        stopWaitingCountdown(rideId);
                        flowSvc.cancelRide(rideId, RideFlowService.CancellationReason.NO_SHOW);
                    }
                }

                /* Toute autre transition => on stoppe la tâche */
                default -> stopWaitingCountdown(rideId);
            }
        }
    }
}
