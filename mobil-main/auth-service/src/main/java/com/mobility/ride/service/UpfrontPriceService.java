// ───────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/UpfrontPriceService.java
//  v2025-08-03 – Fallback « default » pour cityId et currency
// ───────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.config.PricingProperties;
import com.mobility.ride.dto.PriceQuoteRequest;
import com.mobility.ride.dto.PriceQuoteResponse;
import com.mobility.ride.mapper.PricingMapper;
import com.mobility.ride.model.DeliveryZone;
import com.mobility.ride.model.ProductType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.EnumMap;
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

    /* ─────────────── Barème-secours (si YAML manquant) ─────────────── */
    private static final Map<ProductType, Double> GLOBAL_RATE_PER_KM = new EnumMap<>(ProductType.class);
    static {
        GLOBAL_RATE_PER_KM.put(ProductType.X,        2.90);
        GLOBAL_RATE_PER_KM.put(ProductType.POOL,     2.03);
        GLOBAL_RATE_PER_KM.put(ProductType.MOTO,     1.50);
        GLOBAL_RATE_PER_KM.put(ProductType.COMFORT,  3.20);
        GLOBAL_RATE_PER_KM.put(ProductType.XL,       4.00);
        GLOBAL_RATE_PER_KM.put(ProductType.BLACK,    4.50);
        GLOBAL_RATE_PER_KM.put(ProductType.LUX,      5.80);
        GLOBAL_RATE_PER_KM.put(ProductType.DELIVERY, 2.90);
    }

    private static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    /** Devise d’origine : front → YAML → bloc <default>. */
    private String resolveOriginCurrency(PriceQuoteRequest req) {

        if (req.currency() != null && !req.currency().isBlank()) {
            return req.currency().toUpperCase();
        }

        /* 1) tente de trouver la ville correspondante */
        String fromCity = Optional.ofNullable(pricing.getCountry())
                .flatMap(map -> map.values().stream()
                        .filter(c -> c.getCityId().equals(req.cityId()))
                        .findFirst())
                .map(PricingProperties.Country::getCurrency)
                .orElse(null);

        if (fromCity != null) return fromCity;

        /* 2) fallback absolu : bloc « default » */
        return Optional.ofNullable(pricing.getCountry())
                .map(m -> m.get("default"))
                .map(PricingProperties.Country::getCurrency)
                .orElse("USD");                 // ultime secours
    }

    /* ============================================================
       MÉTHODE PRINCIPALE – devis up-front
       ============================================================ */
    public PriceQuoteResponse quote(@Valid PriceQuoteRequest req) {

        /* ───────── 0) Pré-traitement / validation ───────── */
        DeliveryZone zone   = Optional.ofNullable(req.deliveryZone()).orElse(DeliveryZone.LOCAL);
        BigDecimal   weight = Optional.ofNullable(req.weightKg()).orElse(BigDecimal.ZERO);

        if (zone != DeliveryZone.LOCAL) {
            Objects.requireNonNull(req.weightKg(),
                    "Weight (kg) must be provided for interurban and international deliveries");
        }

        String originCurrency = resolveOriginCurrency(req);

        /* ════════════════════════════════════════════════
           1) LIVRAISON (INTERURBAIN / INTERNATIONAL)
           ════════════════════════════════════════════════ */
        if (zone != DeliveryZone.LOCAL) {

            BigDecimal perKg = (zone == DeliveryZone.INTERURBAIN)
                    ? BigDecimal.valueOf(5_000)
                    : BigDecimal.valueOf(10_000);

            BigDecimal baseXaf = weight.multiply(perKg).setScale(2, RoundingMode.HALF_UP);

            BigDecimal fx = originCurrency.equals("XAF")
                    ? BigDecimal.ONE
                    : exchangeRateService.getRate("XAF", originCurrency);

            BigDecimal total = baseXaf.multiply(fx).setScale(2, RoundingMode.HALF_UP);

            return mapper.toPriceQuoteDto(
                    req.cityId(), req.productType(), req.distanceKm(),
                    Math.round(req.durationMin() * 60),
                    baseXaf, BigDecimal.ONE, total, originCurrency,
                    OffsetDateTime.now().plusMinutes(5),
                    weight, zone
            );
        }

        /* ════════════════════════════════════════════════
           2) COURSE CLASSIQUE (LOCAL)
           ════════════════════════════════════════════════ */
        PricingProperties.Country cfg = Optional.ofNullable(pricing.getCountry())
                .flatMap(map -> map.values().stream()
                        .filter(c -> c.getCityId().equals(req.cityId()))
                        .findFirst())
                .orElseGet(() -> {
                    log.warn("[Pricing] cityId={} absent du YAML → fallback « default »",
                            req.cityId());
                    return pricing.getCountry().get("default");
                });

        /* 2-a) Forfait fixe éventuel */
        Double flatFare = Optional.ofNullable(cfg.getFlatFare())
                .map(ff -> ff.get(req.productType().name()))
                .map(Map::values)
                .flatMap(v -> v.stream().findFirst())
                .orElse(null);

        /* 2-b) Tarif par km */
        double ratePerKm = Optional.ofNullable(cfg.getRatePerKm())
                .map(r -> r.get(req.productType().name()))
                .orElseGet(() -> GLOBAL_RATE_PER_KM.getOrDefault(req.productType(), 1.0));

        /* 2-c) Base / surge / total */
        double base  = flatFare != null ? flatFare : req.distanceKm() * ratePerKm;
        double surge = surgeService.getSurgeFactor(cfg.getCityId(), req.productType());
        double total = base * surge;

        log.debug("💰 quote cityId={} type={} km={} base={}{} surge={}× total={}",
                cfg.getCityId(), req.productType(),
                req.distanceKm(), money(base), originCurrency,
                money(surge), money(total));

        return mapper.toPriceQuoteDto(
                cfg.getCityId(), req.productType(), req.distanceKm(),
                Math.round(req.durationMin() * 60),
                money(base), money(surge), money(total),
                originCurrency,
                OffsetDateTime.now().plusMinutes(5),
                weight, zone
        );
    }
}
