// src/main/java/com/mobility/ride/dto/ScheduleRideRequest.java
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
 * Requête pour planifier une course à une date ultérieure ou une livraison.
 * <ul>
 *   <li><strong>riderId</strong> : injecté server-side, jamais envoyé par l’app.</li>
 *   <li><strong>paymentMethodId</strong> : identifiant de la carte/Wallet choisie.</li>
 *   <li><strong>totalFare / currency</strong> : tarif calculé côté mobile et devise ISO-4217.</li>
 *   <li><strong>weightKg</strong> & <strong>deliveryZone</strong> : pour la livraison de colis.</li>
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

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double pickupLat;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double pickupLng;

    @NotNull @DecimalMin("-90.0") @DecimalMax("90.0")
    private Double dropoffLat;

    @NotNull @DecimalMin("-180.0") @DecimalMax("180.0")
    private Double dropoffLng;

    /* ─────────── Produit & options ─────────── */

    /** Type de produit (X, XL, POOL, DELIVERY, etc.). */
    @NotNull
    private String productType;

    /** Liste d’options choisies (facultatif). */
    private List<String> options;

    /* ────────── Livraison / poids ────────── */

    /**
     * Poids du colis en kilogrammes (>= 0.1 kg).
     * – null si ce n’est pas une livraison (deliveryZone == LOCAL).
     */
    @Nullable
    @DecimalMin(value = "0.1", inclusive = true, message = "weightKg doit être ≥ 0.1 pour les livraisons")
    private BigDecimal weightKg;

    /**
     * Zone de livraison.
     * – LOCAL pour un simple ride (peut être null côté JSON, contrôlé en controller).
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
