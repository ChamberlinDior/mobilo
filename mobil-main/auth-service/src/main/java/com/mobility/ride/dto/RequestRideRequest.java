// src/main/java/com/mobility/ride/dto/RequestRideRequest.java
package com.mobility.ride.dto;

import jakarta.validation.constraints.*;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Requête pour une course immédiate (non planifiée).
 *
 * <p>Contient les informations minimales nécessaires pour déclencher
 * une course « à la volée » : géolocalisation, type de service, tarif
 * calculé côté client et, le cas échéant, options additionnelles.</p>
 *
 * <p>Le passager (rider) est identifié via le JWT ; il n’est donc PAS
 * transmis dans cette payload.</p>
 */
@Getter
@Builder
public class RequestRideRequest {

    /** Doit rester nul : le backend déduit le rider depuis le JWT. */
    @Null
    private Long riderId;

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

    /** Type de produit (X, XL, POOL, etc.). */
    @NotBlank
    private String productType;

    /** Montant total estimé (obligatoire). */
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    private BigDecimal totalFare;

    /** Devise du montant (EUR, USD, XAF…). */
    @NotBlank
    private String currency;

    /** Liste d’options choisies par le passager (facultatif). */
    private List<String> options;
}
