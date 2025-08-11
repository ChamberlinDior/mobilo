// ============================
// src/main/java/com/mobility/ride/dto/RideResponse.java
// v2025-10-12 – rider & driver metadata alignés
// ============================
package com.mobility.ride.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Objet retourné après la création, la planification,
 * la re‑planification ou la consultation d’une course / livraison.
 *
 * 🔄 Matrice front ↔ backend :
 * <pre>
 * ┌──────────┬───────────────────┬──────────────────┐
 * │ Audience │  Champs « rider » │  Champs « driver »│
 * ├──────────┼───────────────────┼──────────────────┤
 * │ Driver   │       ✅          │       –          │
 * │ Rider    │       –          │       ✅          │
 * └──────────┴───────────────────┴──────────────────┘
 * </pre>
 * Le DTO expose donc **les deux familles** de champs pour éviter toute
 * divergence entre les applications mobiles.
 */
@Getter
@Builder
public class RideResponse {

    /* ───────────── Identité ───────────── */
    private Long   rideId;
    private String status;

    /* ─────────── Localisation ─────────── */
    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;
    private String pickupAddress;
    private String dropoffAddress;

    /* ───────── Produit & options ──────── */
    private String       productType;
    private List<String> options;

    /* ───────── Planification ─────────── */
    private OffsetDateTime scheduledAt;

    /* ───────── Paiement ─────────────── */
    private Long paymentMethodId;

    /* ───────── Tarification ─────────── */
    private BigDecimal totalFare;
    private String     currency;

    /* ───────── Livraison ────────────── */
    /** Poids du colis en kilogrammes (null si course classique). */
    private BigDecimal weightKg;
    /** Zone de livraison (LOCAL, INTERURBAIN, INTERNATIONAL_…). */
    private String deliveryZone;

    /* ───────── Infos passager (vue chauffeur) ───────── */
    private String riderName;      // « John D. » ou « — »
    private String riderPhone;     // format E.164 ou null
    private String riderPhotoUrl;  // clé objet ou URL CDN (peut être null)

    /* ───────── Infos chauffeur (vue passager) ───────── */
    private String driverName;     // « Alice M. » ou « — »
    private String driverPhone;    // format E.164 ou null
    private String driverPhotoUrl; // idem

    /* ───────── Sécurité & audit ─────── */
    private String         safetyPin;
    private OffsetDateTime createdAt;
}
