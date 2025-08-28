// ============================
// src/main/java/com/mobility/ride/dto/RideResponse.java
// v2025-10-13 – +completedAt (pour HISTORY), rider/driver metadata alignés
// ============================
package com.mobility.ride.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Objet retourné après la création, la planification,
 * la re-planification ou la consultation d’une course / livraison.
 *
 * Le DTO expose à la fois les infos côté rider et côté driver
 * pour éviter toute divergence entre applications mobiles.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    /* ───────── Planification / Historique ───────── */
    /** Date/heure prévue (null pour une ride immédiate). */
    private OffsetDateTime scheduledAt;
    /** ✅ Date/heure de fin réelle (utilisée par le front pour l’onglet HISTORY). */
    private OffsetDateTime completedAt;

    /* ───────── Paiement ─────────────── */
    private Long paymentMethodId;

    /* ───────── Tarification ─────────── */
    private BigDecimal totalFare;
    private String     currency;

    /* ───────── Livraison ────────────── */
    /** Poids du colis en kilogrammes (null si course classique). */
    private BigDecimal weightKg;
    /** Zone de livraison (LOCAL, INTERURBAIN, INTERNATIONAL…). */
    private String deliveryZone;

    /* ───────── Infos passager (vue chauffeur) ───────── */
    private String riderName;      // « John D. » ou « — »
    private String riderPhone;     // format E.164 ou null
    private String riderPhotoUrl;  // clé objet / URL / data:... (peut être null)

    /* ───────── Infos chauffeur (vue passager) ───────── */
    private String driverName;     // « Alice M. » ou « — »
    private String driverPhone;    // format E.164 ou null
    private String driverPhotoUrl; // idem

    /* ───────── Sécurité & audit ─────── */
    private String         safetyPin;
    private OffsetDateTime createdAt;
}
