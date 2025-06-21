// ───────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/ExchangeRateService.java
//  DESC : Conversion hors-ligne XAF ↔ USD / EUR.
//         – Suffisant pour tarification colis interurbain & international.
// ───────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

@Slf4j
@Service
public class ExchangeRateService {

    /* ===== TAUX FIGÉS À METTRE À JOUR PONCTUELLEMENT ===== */

    /** 1 USD = 615 XAF (exemple) */
    private static final BigDecimal XAF_PER_USD = new BigDecimal("615.00");

    /** 1 EUR = 665 XAF (exemple) */
    private static final BigDecimal XAF_PER_EUR = new BigDecimal("665.00");

    private static final int SCALE = 8;                 // précision
    private static final Set<String> SUPPORTED = Set.of("XAF", "USD", "EUR");

    /* =======================  API  ======================= */

    /**
     * Retourne le multiplicateur permettant de convertir 1 {@code from}
     * en {@code to}.  Ex. getRate("XAF","USD") ≈ 0.001626.
     */
    public BigDecimal getRate(String from, String to) {
        from = from.toUpperCase();
        to   = to.toUpperCase();

        if (!SUPPORTED.contains(from) || !SUPPORTED.contains(to)) {
            throw new IllegalArgumentException("Devise non supportée : " + from + " / " + to);
        }
        if (from.equals(to)) return BigDecimal.ONE;      // même devise

        /* ---- étape 1 : convertir 1 « from » en XAF ---- */
        BigDecimal xafOfOne = switch (from) {
            case "XAF" -> BigDecimal.ONE;
            case "USD" -> XAF_PER_USD;
            case "EUR" -> XAF_PER_EUR;
            default    -> throw new IllegalStateException("Devise inattendue : " + from);
        };

        /* ---- étape 2 : convertir ces XAF en « to » ---- */
        BigDecimal toPerXaf = switch (to) {
            case "XAF" -> BigDecimal.ONE;
            case "USD" -> BigDecimal.ONE.divide(XAF_PER_USD, SCALE, RoundingMode.HALF_UP);
            case "EUR" -> BigDecimal.ONE.divide(XAF_PER_EUR, SCALE, RoundingMode.HALF_UP);
            default    -> throw new IllegalStateException("Devise inattendue : " + to);
        };

        BigDecimal rate = xafOfOne.multiply(toPerXaf).setScale(SCALE, RoundingMode.HALF_UP);
        log.debug("💱 FX offline {}→{} = {}", from, to, rate);
        return rate;
    }
}
