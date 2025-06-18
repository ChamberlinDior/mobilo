package com.mobility.ride.service;

import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;         // ← import ajouté
import com.mobility.ride.repository.RideRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.ScheduledFuture;

/**
 * <h2>Gestion des annulations & pénalités</h2>
 *
 * • Planifie automatiquement un « no-show » H+2 min après création (grâce à scheduleNoShowPenalty).<br>
 * • Permet l’annulation explicite par l’utilisateur (cancelRide).<br>
 * • Vérifie et met à jour le statut du ride avant d’appliquer la pénalité (applyPenalty).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CancellationPenaltyService {

    /** Délai de grâce avant facturation (no-show) : 2 minutes. */
    public static final Duration NO_SHOW_GRACE = Duration.ofMinutes(2);

    /** Montant forfaitaire utilisé pour l’exemple : 3.00 XAF. */
    public static final BigDecimal LATE_CANCEL_FEE = new BigDecimal("3.00");

    private final TaskScheduler  scheduler;
    private final RideRepository rideRepo;
    private final PaymentService paymentService;

    /* ════════════════════════════════════════════════════════════════════════
       1) Planification automatique du no-show
       ═══════════════════════════════════════════════════════════════════════ */
    /**
     * Programme l’exécution différée de applyPenalty pour H+2 minutes.
     *
     * @param rideId   ID de la course à surveiller
     * @param fee      montant de la pénalité
     * @param currency devise ISO 4217 (ex. "XAF")
     * @return ScheduledFuture (peut être annulé si besoin)
     */
    public ScheduledFuture<?> scheduleNoShowPenalty(Long rideId,
                                                    BigDecimal fee,
                                                    String currency) {
        return scheduler.schedule(
                () -> applyPenalty(rideId, fee, currency),
                OffsetDateTime.now().plus(NO_SHOW_GRACE).toInstant()
        );
    }

    /* ════════════════════════════════════════════════════════════════════════
       2) Annulation explicite par l’utilisateur
       ═══════════════════════════════════════════════════════════════════════ */
    @Transactional
    public void cancelRide(Long rideId) {
        Ride ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("RIDE_NOT_FOUND"));

        // ➊ Mettre à jour le statut en CANCELLED
        ride.setStatus(RideStatus.CANCELLED);
        rideRepo.save(ride);

        /* ➋ Décide si un frais doit être appliqué.
           ⮕ Gratuit si annulation < 2 min, sinon frais forfaitaire. */
        Duration sinceCreation =
                Duration.between(ride.getCreatedAt(), OffsetDateTime.now());

        if (sinceCreation.compareTo(NO_SHOW_GRACE) <= 0) {
            log.info("✅ Ride {} cancelled within grace period – no fee", rideId);
            return;
        }

        /* ➌ Capture du frais forfaitaire */
        paymentService.captureCancellationFee(
                ride,
                LATE_CANCEL_FEE,
                "XAF"               // adapter la devise si besoin
        );
        log.warn("🚫 Late-cancel fee {} XAF charged for ride {}", LATE_CANCEL_FEE, rideId);
    }

    /* ════════════════════════════════════════════════════════════════════════
       3) Exécution différée (no-show)
       ═══════════════════════════════════════════════════════════════════════ */
    @Transactional
    public void applyPenalty(Long rideId, BigDecimal fee, String currency) {
        Ride ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException("RIDE_NOT_FOUND"));

        /*
         * ➊ Vérifier que la course est toujours éligible au no-show.
         *    – Si le statut a déjà changé (par ex. ACCEPTED, IN_PROGRESS, CANCELLED),
         *      on ne facture pas.
         */
        if (ride.getStatus() != RideStatus.REQUESTED) {
            log.info("Ride {} not in REQUESTED state (current status={}) – skipping no-show penalty",
                    rideId, ride.getStatus());
            return;
        }

        /*
         * ➋ Mettre à jour le statut en CANCELLED (no-show)
         */
        ride.setStatus(RideStatus.CANCELLED);
        rideRepo.save(ride);

        /* ➌ Capture du frais no-show */
        paymentService.captureCancellationFee(ride, fee, currency);
        log.warn("🚫 No-show penalty {} {} charged for ride {}", fee, currency, rideId);
    }
}
