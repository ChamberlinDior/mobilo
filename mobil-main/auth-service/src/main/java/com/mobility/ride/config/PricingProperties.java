// ============================================================================
//  PACKAGE : com.mobility.ride.config
//  FILE    : PricingProperties.java
//  ---------------------------------------------------------------------------
//  Objet de configuration → injecté par Spring Boot (`@ConfigurationProperties`)
//  • 1er niveau : clé pays  (ex. gabon, france, usa …)
//  • cityId      : identifiant interne du marché / ville
//  • currency    : devise ISO (EUR, USD, XAF …)
//  • ratePerKm   : map <ProductType, prix>  — tarification au km
//  • flatFare    : map optionnelle pour les forfaits fixes (ex. MOTO)
//                  structure libre : flatFare.<ProductType>.<code> = montant
//                  exemple : flatFare.MOTO.intra_city = 2000
//  • platformShare : commission (0-1) ; clé “default” appliquée si ProductType manquant
// ============================================================================

package com.mobility.ride.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
@ConfigurationProperties(prefix = "pricing")
@Getter @Setter
public class PricingProperties {

    /** clé = pays (gabon, france, usa, …) */
    private Map<String, Country> country;

    /* ──────────────────────────── */
    /*  Objet imbriqué “Country”   */
    /* ──────────────────────────── */
    @Getter @Setter
    public static class Country {

        /** Identifiant interne (long) du marché / ville. */
        private Long   cityId;

        /** Devise ISO : EUR, USD, XAF, … */
        private String currency;

        /** Tarif au kilomètre par ProductType — ex. { X:1.9, XL:2.66 } */
        private Map<String, Double> ratePerKm;

        /**
         * Forfaits fixes (optionnel).
         * Exemple YAML :<pre>
         *   flatFare:
         *     MOTO:
         *       intra_city:        2000
         *       libreville_akanda: 3000
         * </pre>
         * → flatFare.get("MOTO").get("intra_city")
         */
        private Map<String, Map<String, Double>> flatFare;

        /**
         * Commission plateforme (0 à 1).
         * Exemple :<pre>
         *   platformShare:
         *     default: 0.20
         * </pre>
         * Peut être surchargé par ProductType si besoin.
         */
        private Map<String, Double> platformShare;
    }
}
