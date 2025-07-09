// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/PaymentService.java
//  v2025-09-02 – + captureRideCharge
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.model.Ride;

import java.math.BigDecimal;

/**
 * Port d’accès au prestataire de paiement (Stripe, Paystack, Apple-Pay…).
 *
 * <p>Trois opérations sont exposées :</p>
 *
 * <ol>
 *   <li><b>{@link #authorizeRide}</b> – pré-autoriser (réserver)
 *       le montant estimé au moment de la planification ou de la demande ;</li>
 *   <li><b>{@link #captureCancellationFee}</b> – prélever un
 *       éventuel frais d’annulation / no-show ;</li>
 *   <li><b>{@link #captureRideCharge}</b> – capturer le montant final
 *       à la fin de la course (inclut wait-fees, distance réelle…).</li>
 * </ol>
 *
 * <p>
 * Chaque implémentation (Stripe, stub local, etc.) mappe ces appels
 * vers son SDK ou son API REST spécifique.
 * </p>
 */
public interface PaymentService {

    /* ────────────────────────────────────────────────
       1) Pré-autorisation / réservation
       ──────────────────────────────────────────────── */

    /**
     * Crée (ou met à jour) une autorisation de paiement sur la carte
     * associée à la course.
     *
     * @param ride      course concernée (doit contenir {@code paymentMethodId})
     * @param amount    montant à autoriser (ex. : 15.50)
     * @param currency  devise ISO-4217 (EUR, USD, XAF…)
     */
    void authorizeRide(Ride ride, BigDecimal amount, String currency);

    /* ────────────────────────────────────────────────
       2) Frais d’annulation / no-show
       ──────────────────────────────────────────────── */

    /**
     * Débite un frais d’annulation ou de non-présentation.
     *
     * @param ride      course concernée
     * @param amount    montant à capturer
     * @param currency  devise ISO-4217 (EUR, USD, XAF…)
     */
    void captureCancellationFee(Ride ride, BigDecimal amount, String currency);

    /* ────────────────────────────────────────────────
       3) Capture finale de la course
       ──────────────────────────────────────────────── */

    /**
     * Capture le montant définitif une fois la course terminée.
     * Doit tenir compte :
     * <ul>
     *   <li>du tarif de base&nbsp;;</li>
     *   <li>des frais d’attente ({@code waitFee})&nbsp;;</li>
     *   <li>des éventuels <i>extra distance / duration</i>.</li>
     * </ul>
     *
     * @param ride  course complétée (statut = {@code COMPLETED})
     * @throws Exception si la capture échoue (le service appelant gère le retry)
     */
    void captureRideCharge(Ride ride) throws Exception;
}
