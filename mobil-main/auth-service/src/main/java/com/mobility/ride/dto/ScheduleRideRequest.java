// ───────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/dto/ScheduleRideRequest.java
//  v2025-07-22 – « 0 kg autorisé pour les rides LOCAL »
// ───────────────────────────────────────────────────────────────
package com.mobility.ride.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mobility.ride.model.DeliveryZone;
import jakarta.annotation.Nullable;
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
 * Requête pour planifier une course future ou une livraison de colis.
 *
 * <ul>
 *   <li><strong>riderId</strong> : injecté côté serveur, jamais envoyé par l’app.</li>
 *   <li><strong>paymentMethodId</strong> : identifiant de la carte/Wallet choisie.</li>
 *   <li><strong>totalFare / currency</strong> : tarif calculé côté mobile + devise ISO-4217.</li>
 *   <li><strong>weightKg</strong> :
 *       <ul>
 *         <li><em>LOCAL</em> → peut être <strong>0&nbsp;kg</strong> (non pris en compte).</li>
 *         <li><em>INTERURBAIN / INTERNATIONAL</em> → doit être &ge;&nbsp;0,1&nbsp;kg.</li>
 *       </ul>
 *   </li>
 *   <li><strong>deliveryZone</strong> : LOCAL, INTERURBAIN ou INTERNATIONAL…</li>
 * </ul>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@Builder
public class ScheduleRideRequest {

    /* ─────────── Contexte interne ─────────── */

    /** ID interne du passager (renseigné serveur, donc ignoré en JSON). */
    @JsonIgnore
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

    /** Type de produit (X, XL, POOL, DELIVERY, etc.). */
    @NotNull
    private String productType;

    /** Options facultatives choisies par l’utilisateur. */
    private List<String> options;

    /* ────────── Livraison / poids ────────── */

    /**
     * Poids du colis en kilogrammes.
     * <ul>
     *   <li>≥ 0,0 pour les rides (zone = LOCAL) – 0 signifie “pas de colis”.</li>
     *   <li>≥ 0,1 pour toute livraison (zone ≠ LOCAL).</li>
     * </ul>
     */
    @Nullable
    @DecimalMin(value = "0.0", inclusive = true,
            message = "weightKg doit être ≥ 0.1 kg pour les livraisons")
    private BigDecimal weightKg;

    /**
     * Zone de livraison :
     * <ul>
     *   <li><strong>LOCAL</strong> pour une course classique ;</li>
     *   <li><strong>INTERURBAIN</strong> ou <strong>INTERNATIONAL</strong> pour un colis.</li>
     * </ul>
     * Peut être null dans le JSON ; le contrôleur appliquera LOCAL par défaut.
     */
    @Nullable
    private DeliveryZone deliveryZone;

    /* ─────────── Planification ─────────── */

    @NotNull
    @Future(message = "La date de réservation doit être ultérieure à maintenant")
    private OffsetDateTime scheduledAt;

    /* ─────────── Paiement ─────────── */

    @NotNull
    private Long paymentMethodId;

    /* ───────── Tarification ───────── */

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal totalFare;

    @NotBlank @Size(min = 3, max = 8)
    private String currency;
}
