// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/PaymentServiceImpl.java
//  v2025-10-06 – stub « local » 100 % logs + transferToBank + authorizeWalletTopUp
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.auth.model.User;
import com.mobility.ride.model.Ride;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@Profile("local")
public class PaymentServiceImpl implements PaymentService {

    /** Nom affiché dans les traces. */
    @Value("${app.payments.provider:stub}")
    private String providerName;

    /* ───────────── 1) AUTHORIZE (ride) ───────────── */
    @Override
    public void authorizeRide(Ride ride, BigDecimal amount, String currency) {
        if (ride == null) {
            // garde anti-NPE si jamais appelée par erreur pour un TOP-UP
            log.warn("[PAYMENT {}] authorizeRide appelée avec ride=null — amount={} {}, call ignoré",
                    providerName.toUpperCase(), amount, currency);
            return;
        }
        log.info("[PAYMENT {}] Authorize {} {} — ride #{} (paymentMethodId={})",
                providerName.toUpperCase(), amount, currency,
                ride.getId(), ride.getPaymentMethodId());
        // stub : aucune action réelle
    }

    /* ──────── 2) CANCELLATION FEE (ride) ─────────── */
    @Override
    public void captureCancellationFee(Ride ride, BigDecimal amount, String currency) {
        log.info("[PAYMENT {}] Capture cancellation fee {} {} — ride #{}",
                providerName.toUpperCase(), amount, currency, ride.getId());
        // stub : aucune action réelle
    }

    /* ───────── 3) FINAL CAPTURE (ride) ───────────── */
    @Override
    public void captureRideCharge(Ride ride) {
        BigDecimal amount   = ride.getTotalFare();
        String     currency = ride.getCurrency();
        log.info("[PAYMENT {}] Capture ride charge {} {} — ride #{} (status={})",
                providerName.toUpperCase(), amount, currency,
                ride.getId(), ride.getStatus());
        // stub : aucune action réelle
    }

    /* ────────── 4) PAYOUT DRIVER ──────────── */
    @Override
    public void transferToBank(User driver, BigDecimal amount, String currency) {
        log.info("[PAYMENT {}] Transfer to bank {} {} — driver uid={} (internalId={})",
                providerName.toUpperCase(), amount, currency,
                driver.getExternalUid(), driver.getId());
        // stub : en prod, appel API PSP pour créer un payout
    }

    /* ────────── 5) WALLET TOP-UP (hors ride) ─────── */
    @Override
    public String authorizeWalletTopUp(User user,
                                       BigDecimal amount,
                                       String currency,
                                       Long paymentMethodId,
                                       String idempotencyKey) {
        String ref = "TOPUP-" + System.currentTimeMillis();
        log.info("[PAYMENT {}] Authorize WALLET TOP-UP {} {} — user uid={} (methodId={}, key={}) -> ref={}",
                providerName.toUpperCase(), amount, currency,
                user.getExternalUid(), paymentMethodId, idempotencyKey, ref);
        // stub : aucune action réelle ; on renvoie une référence synthétique
        return ref;
    }
}
