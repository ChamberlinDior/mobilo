// ─────────────────────────────────────────────────────────────────────────────
//  PACKAGE : com.mobility.ride.dto
//  FILE    : SurgeInfoResponse.java
// ----------------------------------------------------------------------------
package com.mobility.ride.dto;

import com.mobility.ride.model.ProductType;

import java.time.OffsetDateTime;

/**
 * Projection « read-only » du multiplicateur Surge renvoyée par
 * <code>GET /pricing/surge</code>.
 *
 * <pre>
 * {
 *   "cityId"      : 123,
 *   "productType" : "X",
 *   "factor"      : 1.8,
 *   "surgeActive" : true,
 *   "windowStart" : "2025-06-01T14:55:00Z",
 *   "windowEnd"   : "2025-06-01T15:25:00Z"
 * }
 * </pre>
 *
 * 👉  Implémenté en <em>record</em> Java 21 → immuable, sérialisé
 * automatiquement par Jackson, et les accesseurs sont générés
 * (<code>factor()</code>, <code>cityId()</code>…).
 */
public record SurgeInfoResponse(

        /* Identifiant de la ville (FK interne) */
        Long cityId,

        /* Produit concerné (X, XL, POOL…) */
        ProductType productType,

        /* Valeur numérique ≥ 1.0  (1.0 ⇒ pas de surge) */
        double factor,

        /* true si factor > 1.0 */
        boolean surgeActive,

        /* Fenêtre de validité du multiplicateur */
        OffsetDateTime windowStart,
        OffsetDateTime windowEnd
) { }
