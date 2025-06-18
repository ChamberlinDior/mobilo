// src/main/java/com/mobility/ride/dto/RideResponse.java
package com.mobility.ride.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Réponse retournée après la création, la planification ou la
 * re-planification d’une course.
 *
 * <p>Le DTO expose désormais <b>les adresses lisibles</b> (pickupAddress
 * &amp; dropoffAddress) en plus des coordonnées, afin que
 * l’application mobile n’ait plus à refaire de géocodage inverse.</p>
 */
@Getter
@Builder
public class RideResponse {

    /* ───────────── Identité ───────────── */

    /** Identifiant unique de la course. */
    private Long rideId;

    /** Statut courant : REQUESTED, SCHEDULED, IN_PROGRESS, COMPLETED… */
    private String status;

    /* ─────────── Localisation ─────────── */

    /** Latitude / longitude du point de prise en charge. */
    private Double pickupLat;
    private Double pickupLng;

    /** Latitude / longitude du point de dépose. */
    private Double dropoffLat;
    private Double dropoffLng;

    /** Adresse lisible du point de prise en charge (nouveau). */
    private String pickupAddress;

    /** Adresse lisible du point de dépose (nouveau). */
    private String dropoffAddress;

    /* ───────── Produit & options ──────── */

    private String        productType;
    private List<String>  options;

    /* ───────── Planification ─────────── */

    /** Date/heure prévue si la course est planifiée, sinon {@code null}. */
    private OffsetDateTime scheduledAt;

    /* ───────── Paiement ─────────────── */

    /** ID du moyen de paiement (carte, wallet, espèces, …). */
    private Long paymentMethodId;

    /* ───────── Tarification ─────────── */

    /** Montant total toutes taxes comprises. */
    private BigDecimal totalFare;

    /** Devise ISO-4217 (USD, EUR, XAF…). */
    private String currency;

    /* ───────── Sécurité & audit ─────── */

    /** Code PIN de sécurité à communiquer au chauffeur. */
    private String safetyPin;

    /** Horodatage de création en base. */
    private OffsetDateTime createdAt;
}
