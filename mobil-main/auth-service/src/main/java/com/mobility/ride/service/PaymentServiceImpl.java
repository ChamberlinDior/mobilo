// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/PaymentServiceImpl.java
//  v2025-10-06 – stub « local » 100 % logs + transferToBank
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.auth.model.User;
import com.mobility.ride.model.Ride;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Implémentation <em>stub</em> du port {@link PaymentService}.
 * <ul>
 *   <li>Aucun appel sortant vers un PSP ; uniquement des logs.</li>
 *   <li>Active quand le profil Spring <strong>local</strong> est sélectionné.</li>
 *   <li>Pour la prod, fournir une implémentation Stripe/Paystack/etc. dans
 *       un autre profil (<code>prod</code>, <code>stripe</code>…).</li>
 * </ul>
 */
@Slf4j
@Service
@Profile("local")
public class PaymentServiceImpl implements PaymentService {

    /** Nom affiché dans les traces. */
    @Value("${app.payments.provider:stub}")
    private String providerName;

    /* ───────────── 1) AUTHORIZE ───────────── */
    @Override
    public void authorizeRide(Ride ride,
                              BigDecimal amount,
                              String currency) {

        log.info("[PAYMENT {}] Authorize {} {} — ride #{} (paymentMethodId={})",
                providerName.toUpperCase(), amount, currency,
                ride.getId(), ride.getPaymentMethodId());

        // stub : aucune action réelle
    }

    /* ──────── 2) CANCELLATION FEE ─────────── */
    @Override
    public void captureCancellationFee(Ride ride,
                                       BigDecimal amount,
                                       String currency) {

        log.info("[PAYMENT {}] Capture cancellation fee {} {} — ride #{}",
                providerName.toUpperCase(), amount, currency, ride.getId());

        // stub : aucune action réelle
    }

    /* ───────── 3) FINAL CAPTURE ───────────── */
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
    public void transferToBank(User driver,
                               BigDecimal amount,
                               String currency) {

        log.info("[PAYMENT {}] Transfer to bank {} {} — driver uid={} (internalId={})",
                providerName.toUpperCase(), amount, currency,
                driver.getExternalUid(), driver.getId());

        // stub : ici on se contenterait, en prod, d’appeler l’API du PSP
        // pour créer un « payout ». En local : rien.
    }
}
