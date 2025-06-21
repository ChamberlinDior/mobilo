// ───────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/UpfrontPriceService.java
//  v2025-07-20  —  « Auto-Currency »
// ───────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.config.PricingProperties;
import com.mobility.ride.dto.PriceQuoteRequest;
import com.mobility.ride.dto.PriceQuoteResponse;
import com.mobility.ride.mapper.PricingMapper;
import com.mobility.ride.model.DeliveryZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpfrontPriceService {

    private final SurgePricingService surgeService;
    private final PricingMapper       mapper;
    private final PricingProperties   pricing;
    private final ExchangeRateService exchangeRateService;

    private static final double DEFAULT_RATE_PER_KM = 1.0;

    /* ============================================================
       UTILITAIRES
       ============================================================ */
    private static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Détermine la devise d’origine **sans intervention du front** :
     * <ol>
     *   <li>Si le front fournit tout de même {@code currency}, on la respecte.</li>
     *   <li>Sinon on lit, via le YAML de tarification, la devise associée au
     *       {@code cityId} de la requête.</li>
     *   <li>Fallback : XAF (Gabon).</li>
     * </ol>
     */
    private String resolveOriginCurrency(PriceQuoteRequest req) {

        /* 1) Front explicite → on garde. */
        if (req.currency() != null && !req.currency().isBlank()) {
            return req.currency().toUpperCase();
        }

        /* 2) YAML : cityId → currency. */
        return Optional.ofNullable(pricing.getCountry())
                .flatMap(map -> map.values().stream()
                        .filter(c -> c.getCityId().equals(req.cityId()))
                        .findFirst())
                .map(PricingProperties.Country::getCurrency)
                .orElse("XAF");                          // 3) fallback
    }

    /* ============================================================
       MÉTHODE PRINCIPALE
       ============================================================ */
    public PriceQuoteResponse quote(@Valid PriceQuoteRequest req) {

        /* ───── Lecture & validations initiales ───── */
        DeliveryZone zone   = Optional.ofNullable(req.deliveryZone())
                .orElse(DeliveryZone.LOCAL);
        BigDecimal weight   = Optional.ofNullable(req.weightKg())
                .orElse(BigDecimal.ZERO);

        if (zone != DeliveryZone.LOCAL) {
            Objects.requireNonNull(req.weightKg(),
                    "Weight (kg) must be provided for interurban and international deliveries");
        }

        /* ───── Devise d’origine (auto-détection) ───── */
        String originCurrency = resolveOriginCurrency(req);

        /* ═════════════════════════════════════════════
           1) LIVRAISON (INTERURBAIN ou INTERNATIONAL)
           ═════════════════════════════════════════════ */
        if (zone != DeliveryZone.LOCAL) {

            BigDecimal perKg   = (zone == DeliveryZone.INTERURBAIN)
                    ? BigDecimal.valueOf(5_000)   // Interurbain
                    : BigDecimal.valueOf(10_000); // International

            BigDecimal baseXaf = weight.multiply(perKg)
                    .setScale(2, RoundingMode.HALF_UP);

            /* Conversion XAF → originCurrency (sauf si déjà XAF) */
            BigDecimal rate = originCurrency.equals("XAF")
                    ? BigDecimal.ONE
                    : exchangeRateService.getRate("XAF", originCurrency);

            BigDecimal totalFare = baseXaf.multiply(rate)
                    .setScale(2, RoundingMode.HALF_UP);

            return mapper.toPriceQuoteDto(
                    req.cityId(),
                    req.productType(),
                    req.distanceKm(),
                    Math.round(req.durationMin() * 60),
                    baseXaf,                 // base toujours en XAF
                    BigDecimal.ONE,          // pas de surge sur les colis
                    totalFare,
                    originCurrency,
                    OffsetDateTime.now().plusMinutes(5),
                    weight,
                    zone
            );
        }

        /* ═════════════════════════════════════════════
           2) COURSE CLASSIQUE (LOCAL) – logique inchangée
           ═════════════════════════════════════════════ */
        PricingProperties.Country cfg = Optional.ofNullable(pricing.getCountry())
                .flatMap(map -> map.values().stream()
                        .filter(c -> c.getCityId().equals(req.cityId()))
                        .findFirst())
                .orElse(null);

        String currency = originCurrency;   // ← déjà résolu

        Double flatFare = Optional.ofNullable(cfg)
                .map(PricingProperties.Country::getFlatFare)
                .map(ff -> ff.get(req.productType().name()))
                .map(Map::values)
                .flatMap(v -> v.stream().findFirst())
                .orElse(null);

        double ratePerKm = Optional.ofNullable(cfg)
                .map(PricingProperties.Country::getRatePerKm)
                .map(r -> r.get(req.productType().name()))
                .orElse(DEFAULT_RATE_PER_KM);

        double base  = (flatFare != null ? flatFare : req.distanceKm() * ratePerKm);
        double surge = surgeService.getSurgeFactor(
                Optional.ofNullable(cfg).map(PricingProperties.Country::getCityId)
                        .orElse(req.cityId()),
                req.productType());
        double total = base * surge;

        log.debug("💰 quote cityId={} type={} km={} base={}{} surge={}× total={}",
                req.cityId(), req.productType(),
                req.distanceKm(), money(base), currency,
                money(surge), money(total));

        return mapper.toPriceQuoteDto(
                Optional.ofNullable(cfg).map(PricingProperties.Country::getCityId)
                        .orElse(req.cityId()),
                req.productType(),
                req.distanceKm(),
                Math.round(req.durationMin() * 60),
                money(base),
                money(surge),
                money(total),
                currency,
                OffsetDateTime.now().plusMinutes(5),
                weight,
                zone
        );
    }
}
