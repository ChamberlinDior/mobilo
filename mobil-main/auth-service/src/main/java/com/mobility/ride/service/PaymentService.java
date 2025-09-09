// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/PaymentService.java
//  v2025-10-06 – + transferToBank (payout chauffeur) + authorizeWalletTopUp
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.auth.model.User;
import com.mobility.ride.model.Ride;

import java.math.BigDecimal;

/**
 * Port d’accès au prestataire de paiement (Stripe, Paystack, Apple-Pay…).
 *
 * <p>Opérations supportées :</p>
 * <ol>
 *   <li><b>{@link #authorizeRide}</b> – pré-autoriser le montant estimé avant/pendant la course ;</li>
 *   <li><b>{@link #captureCancellationFee}</b> – prélever d’éventuels frais d’annulation ;</li>
 *   <li><b>{@link #captureRideCharge}</b> – capturer le tarif final à la fin de la course ;</li>
 *   <li><b>{@link #transferToBank}</b> – virer le solde d’un chauffeur vers sa banque/mobile money ;</li>
 *   <li><b>{@link #authorizeWalletTopUp}</b> – autoriser un TOP-UP du wallet (hors ride) et retourner une référence PSP.</li>
 * </ol>
 */
public interface PaymentService {

    /* ───────────────────────────────────────────────
       1) Pré-autorisation / réservation (course)
       ─────────────────────────────────────────────── */
    void authorizeRide(Ride ride, BigDecimal amount, String currency);

    /* ───────────────────────────────────────────────
       2) Frais d’annulation / no-show (course)
       ─────────────────────────────────────────────── */
    void captureCancellationFee(Ride ride, BigDecimal amount, String currency);

    /* ───────────────────────────────────────────────
       3) Capture finale de la course
       ─────────────────────────────────────────────── */
    void captureRideCharge(Ride ride) throws Exception;

    /* ───────────────────────────────────────────────
       4) Payout chauffeur (retrait)
       ─────────────────────────────────────────────── */
    void transferToBank(User driver, BigDecimal amount, String currency);

    /* ───────────────────────────────────────────────
       5) TOP-UP wallet (hors ride)
       ───────────────────────────────────────────────
       @param user            propriétaire du wallet
       @param amount          montant à autoriser (dans la devise fournie)
       @param currency        devise de l’autorisation (ex: "XAF", "EUR", "USD")
       @param paymentMethodId identifiant interne du moyen de paiement (nullable)
       @param idempotencyKey  clé d’idempotence transmise au PSP si applicable (nullable)
       @return                 référence de l’autorisation chez le PSP (ou synthétique en local)
     */
    String authorizeWalletTopUp(User user,
                                BigDecimal amount,
                                String currency,
                                Long paymentMethodId,
                                String idempotencyKey);
}
