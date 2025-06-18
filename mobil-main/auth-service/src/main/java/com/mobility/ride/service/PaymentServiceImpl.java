/* ------------------------------------------------------------------
 * Implémentation « noop » du port de paiement.
 *  • 100 % « stub » : se contente d’écrire des logs, sans appeler
 *    la moindre passerelle bancaire.
 *  • Activée uniquement quand le profil Spring ► local ◄ est actif.
 *    En production, basculez sur une implémentation Stripe / Paystack
 *    (ou changez simplement de @Profile).
 * ------------------------------------------------------------------ */
package com.mobility.ride.service;

import com.mobility.ride.model.Ride;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Slf4j
@Service
@Profile("local")   // -> désactivé dès qu’un autre profil est sélectionné
public class PaymentServiceImpl implements PaymentService {

    /**
     * Nom symbolique du prestataire (pour les logs).
     * On laisse la propriété overridable :  spring.profiles.active=local
     * + app.payments.provider=stripe   donnera des logs « [PAYMENT STRIPE] … ».
     */
    @Value("${app.payments.provider:stub}")
    private String providerName;

    /* ──────────────────────────────────────────────────────────────
       1) Pré-autorisation (planification / demande immédiate)
       ────────────────────────────────────────────────────────────── */
    @Override
    public void authorizeRide(Ride ride,
                              BigDecimal amount,
                              String currency) {

        log.info("[PAYMENT {}] Authorize {} {} — ride #{} (paymentMethodId={})",
                providerName.toUpperCase(), amount, currency, ride.getId(),
                ride.getPaymentMethodId());

        /* ---------------------------------------------------------
         * En environnement « local » on s’arrête ici.
         *  ‣ En prod, appelez ici l’API de votre PSP :
         *      - Stripe   → PaymentIntents + confirmation automatique
         *      - Paystack → Transaction initialise & charge_authorization
         *      - …         etc.
         * --------------------------------------------------------- */
    }

    /* ──────────────────────────────────────────────────────────────
       2) Frais d’annulation / no-show
       ────────────────────────────────────────────────────────────── */
    @Override
    public void captureCancellationFee(Ride ride,
                                       BigDecimal amount,
                                       String currency) {

        log.info("[PAYMENT {}] Capture cancellation fee {} {} — ride #{}",
                providerName.toUpperCase(), amount, currency, ride.getId());

        // TODO (prod) : appel réel à la passerelle pour capturer les fonds.
    }
}
