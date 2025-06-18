// src/main/java/com/mobility/ride/dto/ScheduleRideRequest.java
package com.mobility.ride.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Requête pour planifier une course à une date ultérieure.
 * <ul>
 *   <li><strong>riderId</strong> : injecté serveur-side, jamais envoyé par l’app.</li>
 *   <li><strong>paymentMethodId</strong> : identifiant de la carte/Wallet choisie
 *       (ex. : 42 pour la Visa se terminant par 4242).</li>
 *   <li><strong>totalFare / currency</strong> : tarif calculé côté mobile et devise ISO‑4217.</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@Builder
public class ScheduleRideRequest {

    /* ─────────── Contexte interne ─────────── */

    /** ID interne du passager (ajouté côté serveur, donc ignoré en JSON). */
    @JsonIgnore
    private Long riderId;

    /* ─────────── Points de trajet ─────────── */

    /** Latitude du point de prise en charge. */
    @NotNull
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double pickupLat;

    /** Longitude du point de prise en charge. */
    @NotNull
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double pickupLng;

    /** Latitude du point de dépose. */
    @NotNull
    @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double dropoffLat;

    /** Longitude du point de dépose. */
    @NotNull
    @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double dropoffLng;

    /* ─────────── Produit & options ─────────── */

    /** Type de produit (X, XL, POOL, etc.). */
    @NotNull
    private String productType;

    /** Liste d’options choisies (facultatif). */
    private List<String> options;

    /* ─────────── Planification ─────────── */

    /** Date et heure prévues (doit être future). */
    @NotNull
    @Future(message = "La date de réservation doit être ultérieure à maintenant")
    private OffsetDateTime scheduledAt;

    /* ─────────── Paiement ─────────── */

    /** Identifiant du moyen de paiement choisi (carte, Apple Pay, …). */
    @NotNull
    private Long paymentMethodId;

    /* ─────────── Tarification ─────────── */

    /** Montant total TTC calculé côté mobile. */
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal totalFare;

    /** Devise ISO‑4217 (USD, EUR, XAF…). */
    @NotBlank
    @Size(min = 3, max = 8)
    private String currency;
}
