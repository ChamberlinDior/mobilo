// ============================================================================
//  FILE    : PriceQuoteRequest.java
//  PACKAGE : com.mobility.ride.dto
//  ---------------------------------------------------------------------------
//  – La devise `currency` n’est plus bloquante : elle peut être vide ou nulle
//    car le backend la remplacera après géolocalisation.
//  – Ajout v2025-07-01 : prise en charge des livraisons interurbain & international.
// ============================================================================
package com.mobility.ride.dto;

import com.mobility.ride.model.DeliveryZone;
import com.mobility.ride.model.LatLng;
import com.mobility.ride.model.ProductType;
import com.mobility.ride.model.RideOption;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PriceQuoteRequest(

        /* ─────────────────── EXIGÉS ─────────────────── */
        @NotNull
        Long        cityId,

        @NotNull
        ProductType productType,

        @NotNull
        LatLng      pickup,

        @NotNull
        LatLng      dropoff,

        @NotNull
        @Positive(message = "distanceKm doit être strictement positif")
        Double      distanceKm,

        @NotNull
        @Positive(message = "durationMin doit être strictement positif")
        Integer     durationMin,

        /* ─────────────────── OPTIONNELS ─────────────── */
        /**
         * Devise ISO-4217 (« EUR », « USD », …).
         * Peut être null ou "" – corrigée par le contrôleur via géolocalisation.
         */
        @Size(max = 3, message = "currency doit être ISO-4217 (3 lettres)")
        @Nullable
        String      currency,

        /**
         * Facteur de surge déjà calculé (utile pour les tests).
         */
        @Nullable
        BigDecimal  surgeFactor,

        /**
         * Options facultatives (bagages, siège enfant, …).
         */
        @Nullable
        RideOption[] options,

        /* ────────── NOUVEAUX CHAMPS POUR LIVRAISON ────────── */
        /**
         * Poids du colis en kilogrammes.
         * – ≥ 0.1 pour livraison interurbain/international.
         * – null pour un trajet ride classique (LOCAL).
         */
        @Nullable
        @DecimalMin(value = "0.1", inclusive = true,
                message = "weightKg doit être ≥ 0.1 pour les livraisons interurbain/international")
        BigDecimal  weightKg,

        /**
         * Zone de livraison.
         * – LOCAL pour un ride normal.
         * – INTERURBAIN ou INTERNATIONAL_* pour livraisons.
         * – null pour un trajet ride classique (LOCAL, pris par défaut).
         */
        @Nullable
        DeliveryZone deliveryZone

) {}
