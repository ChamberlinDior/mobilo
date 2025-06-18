// src/main/java/com/mobility/ride/service/PaymentService.java
package com.mobility.ride.service;

import com.mobility.ride.model.Ride;

import java.math.BigDecimal;

/**
 * Port d’accès au prestataire de paiement (Stripe, Paystack, Apple-Pay…).
 *
 * <p>Deux opérations sont actuellement nécessaires :</p>
 * <ul>
 *   <li><strong>authorizeRide</strong> : pré-autoriser (ou réserver)
 *       le montant estimé au moment de la planification ou de la demande.</li>
 *   <li><strong>captureCancellationFee</strong> : prélever
 *       un éventuel frais d’annulation / no-show.</li>
 * </ul>
 *
 * <p>Chaque implémentation (Stripe, stub local, etc.) décide de la
 * manière dont elle mappe ces appels vers son SDK ou son API REST.</p>
 */
public interface PaymentService {

    /* ──────────────────────────────────────────────────────────
       1) Pré-autorisation / réservation
       ────────────────────────────────────────────────────────── */

    /**
     * Crée (ou met à jour) une autorisation de paiement sur la carte
     * associée à la course.
     *
     * @param ride     la course concernée (doit contenir paymentMethodId)
     * @param amount   montant à autoriser (ex. : 15.50)
     * @param currency devise ISO 4217 (EUR, USD, XAF…)
     */
    void authorizeRide(Ride ride, BigDecimal amount, String currency);

    /* ──────────────────────────────────────────────────────────
       2) Frais d’annulation / no-show
       ────────────────────────────────────────────────────────── */

    /**
     * Débite un frais d’annulation ou de non-présentation.
     *
     * @param ride     course concernée
     * @param amount   montant à capturer
     * @param currency devise ISO 4217 (EUR, USD, XAF…)
     */
    void captureCancellationFee(Ride ride, BigDecimal amount, String currency);
}
