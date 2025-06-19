// ============================================================================
//  FILE    : PriceQuoteResponse.java
//  PACKAGE : com.mobility.ride.dto
//  ---------------------------------------------------------------------------
//  DTO « devis tarifaire up-front » renvoyé au mobile.
//
//  Nouveautés (v2025-06):
//   • platformShare     – pourcentage retenu par Proximo (reporting)
//   • ratePerKmUsed     – tarif/km appliqué (null si forfait)
//   • flatFareLabel     – identifie un forfait (ex. "MOTO.intra_city"), null sinon
//  Ajouts v2025-07-01 pour livraisons interurbain & international:
//   • weightKg         – poids du colis (kg)
//   • deliveryZone     – zone de livraison (LOCAL, INTERURBAIN, INTERNATIONAL_USA, INTERNATIONAL_FRANCE)
// ============================================================================
package com.mobility.ride.dto;

import com.mobility.ride.model.DeliveryZone;
import com.mobility.ride.model.ProductType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceQuoteResponse {

    /* ───────────────────────── CONTEXTE ───────────────────────── */
    /** Identifiant interne du marché (ex. 10 = Libreville). */
    private Long          cityId;

    /** Service choisi : X, XL, POOL, MOTO, … */
    private ProductType   productType;

    /* ────────── LIVRAISON (nouveau) ────────── */
    /** Poids du colis (kg). */
    private BigDecimal    weightKg;

    /** Zone de livraison. */
    private DeliveryZone  deliveryZone;

    /* ─────────────────── MÉTRIQUES TRAJET ─────────────────────── */
    /** Distance estimée (km). */
    private Double        distanceKm;
    /** Durée estimée (secondes). */
    private Long          durationSec;

    /* ───────────────────── TARIFICATION ───────────────────────── */
    /** Tarif de base (distance × ratePerKm **ou** forfait). */
    private BigDecimal    baseFare;

    /** Multiplicateur « surge » appliqué (≥ 1). */
    private BigDecimal    surgeFactor;

    /** Tarif final (baseFare × surgeFactor). */
    private BigDecimal    totalFare;

    /** Tarif/km réellement utilisé – null si forfait. */
    private BigDecimal    ratePerKmUsed;

    /** Libellé forfait (ex. “MOTO.intra_city”) ou null. */
    private String        flatFareLabel;

    /** Part Proximo (0-1). Exemple : 0.20 ⇒ 20 %. */
    private BigDecimal    platformShare;

    /** Devise ISO-4217 (EUR, USD, XAF…). */
    private String        currency;

    /* ───────────────────────── MÉTA ───────────────────────────── */
    /** Moment d’expiration du devis. */
    private OffsetDateTime expiresAt;

    /** Toujours TRUE pour un devis up-front. */
    private Boolean       upfront;
}
