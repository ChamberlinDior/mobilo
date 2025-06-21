// ───────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/dto/RequestRideRequest.java
//  v2025-07-22 – « 0 kg permis pour les rides LOCAL »
// ───────────────────────────────────────────────────────────────
package com.mobility.ride.dto;

import com.mobility.ride.model.DeliveryZone;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Requête pour déclencher une course immédiate (ride) ou une livraison.
 *
 * <ul>
 *   <li><strong>weightKg</strong> :
 *       <ul>
 *         <li>≥ 0,0 kg pour les rides simples (zone = LOCAL) — 0 signifie « pas de colis ».</li>
 *         <li>≥ 0,1 kg pour les livraisons (zone ≠ LOCAL).</li>
 *       </ul>
 *   </li>
 *   <li>Le passager est authentifié via le JWT ; <code>riderId</code> est donc <em>null</em> côté front.</li>
 * </ul>
 */
@Getter
@Builder
public class RequestRideRequest {

    /* ─────────── Contexte ─────────── */

    /** ID interne du passager (injecté côté serveur). */
    @Null
    private Long riderId;

    /* ─────────── Points de trajet ─────────── */

    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")
    private Double pickupLat;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double pickupLng;

    @NotNull @DecimalMin("-90.0")  @DecimalMax("90.0")
    private Double dropoffLat;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double dropoffLng;

    /* ─────────── Produit & options ─────────── */

    /** Type de service (X, XL, POOL, DELIVERY…). */
    @NotBlank
    private String productType;

    /** Options facultatives sélectionnées par l’utilisateur. */
    private List<String> options;

    /* ────────── Livraison / poids ────────── */

    /**
     * Poids du colis en kilogrammes.
     * <ul>
     *   <li>0 kg autorisé pour zone = LOCAL.</li>
     *   <li>&ge; 0,1 kg requis pour toute livraison (INTERURBAIN / INTERNATIONAL).</li>
     * </ul>
     */
    @Nullable
    @DecimalMin(value = "0.0", inclusive = true,
            message = "weightKg doit être ≥ 0.1 pour les livraisons")
    private BigDecimal weightKg;

    /**
     * Zone de service : LOCAL, INTERURBAIN ou INTERNATIONAL.
     * Peut être null dans le JSON (défaut LOCAL appliqué en contrôleur).
     */
    @NotNull
    private DeliveryZone deliveryZone;

    /* ─────────── Tarification ─────────── */

    /** Montant total fourni par l’app (obligatoire). */
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal totalFare;

    /** Devise ISO-4217 (EUR, USD, XAF…). */
    @NotBlank
    private String currency;
}
