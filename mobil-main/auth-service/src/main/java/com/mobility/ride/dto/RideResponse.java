// src/main/java/com/mobility/ride/dto/RideResponse.java
package com.mobility.ride.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Réponse retournée après la création, la planification ou la
 * re-planification d’une course ou livraison.
 *
 * <p>Le DTO expose désormais :
 *  • les adresses lisibles (pickupAddress & dropoffAddress),
 *  • le poids du colis et la zone de livraison pour les envois.</p>
 */
@Getter
@Builder
public class RideResponse {

    /* ───────────── Identité ───────────── */
    private Long rideId;
    private String status;

    /* ─────────── Localisation ─────────── */
    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;
    private String pickupAddress;
    private String dropoffAddress;

    /* ───────── Produit & options ──────── */
    private String        productType;
    private List<String>  options;

    /* ───────── Planification ─────────── */
    private OffsetDateTime scheduledAt;

    /* ───────── Paiement ─────────────── */
    private Long          paymentMethodId;

    /* ───────── Tarification ─────────── */
    private BigDecimal    totalFare;
    private String        currency;

    /* ───────── Livraison ────────────── */
    /** Poids du colis en kilogrammes (null si course classique). */
    private BigDecimal    weightKg;
    /** Zone de livraison (LOCAL, INTERURBAIN, INTERNATIONAL_USA, INTERNATIONAL_FRANCE). */
    private String        deliveryZone;

    /* ───────── Sécurité & audit ─────── */
    private String        safetyPin;
    private OffsetDateTime createdAt;
}
