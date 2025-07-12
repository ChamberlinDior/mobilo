// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/PaymentService.java
//  v2025-10-06 – + transferToBank (payout chauffeur)
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
 *   <li><b>{@link #authorizeRide}</b> – pré-autoriser le montant estimé
 *       avant ou pendant la course ;</li>
 *   <li><b>{@link #captureCancellationFee}</b> – prélever un éventuel
 *       frais d’annulation ;</li>
 *   <li><b>{@link #captureRideCharge}</b> – capturer le tarif final une fois
 *       la course terminée ;</li>
 *   <li><b>{@link #transferToBank}</b> – virer le solde d’un chauffeur
 *       vers son compte bancaire ou mobile-money.</li>
 * </ol>
 */
public interface PaymentService {

    /* ───────────────────────────────────────────────
       1) Pré-autorisation / réservation
       ─────────────────────────────────────────────── */
    void authorizeRide(Ride ride, BigDecimal amount, String currency);

    /* ───────────────────────────────────────────────
       2) Frais d’annulation / no-show
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
}
