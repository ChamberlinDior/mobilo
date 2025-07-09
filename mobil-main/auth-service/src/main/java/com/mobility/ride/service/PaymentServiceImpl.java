// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/PaymentServiceImpl.java
//  v2025-09-02 – stub « local » 100 % logs
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.model.Ride;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Implémentation <em>stub</em> (no-op) du port {@link PaymentService}.
 * <ul>
 *   <li>Aucun appel sortant vers un PSP ; uniquement des logs.</li>
 *   <li>Activée quand le profil Spring <strong>local</strong> est sélectionné.</li>
 *   <li>En production, fournir une implémentation Stripe / Paystack /
 *       Apple-Pay… dans un autre profil (<code>prod</code>, <code>stripe</code>, …).</li>
 * </ul>
 */
@Slf4j
@Service
@Profile("local")
public class PaymentServiceImpl implements PaymentService {

    /** Libellé du provider à afficher dans les traces. */
    @Value("${app.payments.provider:stub}")
    private String providerName;

    /* ───────────────────────────── 1) AUTHORIZE ────────────────────────── */
    @Override
    public void authorizeRide(Ride ride,
                              BigDecimal amount,
                              String currency) {

        log.info("[PAYMENT {}] Authorize {} {} — ride #{} (paymentMethodId={})",
                providerName.toUpperCase(), amount, currency,
                ride.getId(), ride.getPaymentMethodId());

        // (stub) : rien d’autre à faire.
    }

    /* ──────────────────────── 2) CANCELLATION FEE ─────────────────────── */
    @Override
    public void captureCancellationFee(Ride ride,
                                       BigDecimal amount,
                                       String currency) {

        log.info("[PAYMENT {}] Capture cancellation fee {} {} — ride #{}",
                providerName.toUpperCase(), amount, currency, ride.getId());

        // (stub) : aucune requête PSP.
    }

    /* ───────────────────────── 3) FINAL CAPTURE ───────────────────────── */
    @Override
    public void captureRideCharge(Ride ride) {

        BigDecimal amount   = ride.getTotalFare(); // inclut waitFee + extras
        String     currency = ride.getCurrency();

        log.info("[PAYMENT {}] Capture ride charge {} {} — ride #{} (status={})",
                providerName.toUpperCase(), amount, currency,
                ride.getId(), ride.getStatus());

        // (stub) : pas d’appel externe.
    }
}
