// ============================================================================
//  FILE    : PriceQuoteRequest.java
//  PACKAGE : com.mobility.ride.dto
//  ---------------------------------------------------------------------------
//  – La devise `currency` n’est plus bloquante : elle peut être vide ou nulle
//    car le backend la remplacera après géolocalisation.
//
//  – Tous les autres champs gardent leur validation stricte.
// ============================================================================
package com.mobility.ride.dto;

import com.mobility.ride.model.LatLng;
import com.mobility.ride.model.ProductType;
import com.mobility.ride.model.RideOption;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record PriceQuoteRequest(

        /* ─────────────────── EXIGÉS ─────────────────── */
        @NotNull           Long        cityId,
        @NotNull           ProductType productType,
        @NotNull           LatLng      pickup,
        @NotNull           LatLng      dropoff,
        @Positive          Double      distanceKm,
        @Positive          Integer     durationMin,

        /* ─────────────────── OPTIONNELS ─────────────── */
        /** Devise ISO (« EUR », « USD », …).
         *  Peut être null/"" – elle sera corrigée par le contrôleur. */
        @Size(max = 3, message = "currency doit être ISO-4217 (3 lettres)")
        String              currency,

        /** Facteur surge déjà calculé (utile pour les tests). */
        BigDecimal          surgeFactor,

        /** Options facultatives (bagages, siège enfant, …). */
        RideOption[]        options
) {}
