/* ------------------------------------------------------------------
 *  RideLifecycleService – passage d’une course à COMPLETED
 * ------------------------------------------------------------------
 *  • Capture / règlement via PaymentService.
 *  • Si le moyen de paiement est CASH → historise aussitôt
 *    un débit « CASH_PAYMENT » (historique uniquement ; n’impacte
 *    PAS le solde utilisateur).
 *  • Associe la transaction au rider à l’aide de son **UID**
 *    (clé utilisée par l’API Wallet), avec repli sur l’id numérique
 *    si le champ riderUid n’était pas encore alimenté.
 * ------------------------------------------------------------------ */
package com.mobility.ride.service;

import com.mobility.auth.model.User;
import com.mobility.auth.model.WalletTransaction;
import com.mobility.auth.model.enums.PaymentProvider;
import com.mobility.auth.model.enums.WalletTxnType;              // + CASH_PAYMENT
import com.mobility.auth.repository.PaymentMethodRepository;
import com.mobility.auth.repository.UserRepository;
import com.mobility.auth.repository.WalletTransactionRepository;
import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;
import com.mobility.ride.repository.RideRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class RideLifecycleService {

    private final RideRepository              rideRepo;
    private final PaymentMethodRepository     pmRepo;
    private final WalletTransactionRepository txnRepo;
    private final UserRepository              userRepo;
    private final PaymentService              paymentSvc;   // Stripe / stub / …

    /* ═══════════════════════════════════════════════════════
       COMPLETE RIDE
       ═══════════════════════════════════════════════════════ */
    @Transactional
    public void completeRide(Long rideId, BigDecimal finalFare) {

        /* ─── 1. Chargement & validations de base ─────────── */
        Ride ride = rideRepo.findById(rideId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Ride not found – id=" + rideId));

        if (ride.getStatus() != RideStatus.IN_PROGRESS) {
            throw new IllegalStateException("Ride must be IN_PROGRESS to complete");
        }

        ride.setStatus(RideStatus.COMPLETED);
        ride.setTotalFare(finalFare);

        /* ─── 2. Capture / règlement PSP ──────────────────── */
        try {
            paymentSvc.captureCancellationFee(ride, finalFare, ride.getCurrency());
        } catch (Exception ex) {
            log.error("[PAYMENT] Capture failed for ride #{} : {}", rideId, ex.getMessage());
            // TODO : stratégie de retry / flag pour investigation
        }

        /* ─── 3. Détermination du provider ──────────────────
           • Aucune carte (paymentMethodId == null)  ➜ CASH
           • Sinon on regarde la carte stockée       ➜ provider réel
         ---------------------------------------------------- */
        final PaymentProvider provider;
        if (ride.getPaymentMethodId() == null) {
            provider = PaymentProvider.CASH;
        } else {
            provider = pmRepo.findById(ride.getPaymentMethodId())
                    .map(pm -> pm.getProvider())
                    .orElse(PaymentProvider.STRIPE);      // fallback
        }

        /* ─── 4. Journaliser un CASH_PAYMENT au wallet ───── */
        if (provider == PaymentProvider.CASH) {

            // a) Résolution du rider : UID prioritaire, sinon id numérique
            User rider;
            try {
                rider = userRepo.findByUid(ride.getRiderUid())         // clé « lecture »
                        .orElseThrow();
            } catch (Exception ignore) {
                rider = userRepo.findById(ride.getRiderId())           // repli
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Rider not found – id=" + ride.getRiderId()));
            }

            // b) Insertion dans wallet_transactions
            WalletTransaction txn = WalletTransaction.builder()
                    .user     (rider)
                    .type     (WalletTxnType.CASH_PAYMENT)
                    .amount   (finalFare.negate())       // débit
                    .currency (ride.getCurrency())
                    .reference("RIDE-" + ride.getId())
                    .build();

            txnRepo.save(txn);
            log.info("[WALLET] CASH_PAYMENT journalised – ride #{}", ride.getId());
        }

        /* ─── 5. Hibernate flush : la table rides sera mise à jour ── */
    }
}
