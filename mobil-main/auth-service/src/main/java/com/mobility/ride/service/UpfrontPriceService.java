// ============================================================================
//  PACKAGE : com.mobility.ride.service
//  FILE    : UpfrontPriceService.java               (v2025-06-11)
// ============================================================================
//  Rôle :
//    • Calcule le devis « up-front ».
//    • Priorité tarif : flatFare  ▶ ratePerKm  ▶ defaultRatePerKm.
//    • Fallback #1  : recherche par cityId (id marché).
//    • Fallback #2  : recherche par devise (currency) si cityId inconnu.
//    • Fallback #3  : neutre 1 × distance (ou defaultRatePerKm).
//    • Applique le multiplicateur surge.
// ============================================================================
package com.mobility.ride.service;

import com.mobility.ride.config.PricingProperties;
import com.mobility.ride.dto.PriceQuoteRequest;
import com.mobility.ride.dto.PriceQuoteResponse;
import com.mobility.ride.mapper.PricingMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UpfrontPriceService {

    /* ───────────────────────── DEPENDANCES ───────────────────────────── */
    private final SurgePricingService surgeService;
    private final PricingMapper       mapper;
    private final PricingProperties   pricing;

    /* ─────────────── PARAMÈTRE DE SECOURS GLOBALE ────────────────────── */
    private static final double DEFAULT_RATE_PER_KM = 1.0;

    /* Helper arrondi deux décimales */
    private static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    /* ─────────────────── MÉTHODE PRINCIPALE ──────────────────────────── */
    public PriceQuoteResponse quote(@Valid PriceQuoteRequest req) {

        /* 1) — Recherche PRIMARY sur cityId ———————————————— */
        PricingProperties.Country cfg = Optional.ofNullable(pricing.getCountry())
                .flatMap(map -> map.values()
                        .stream()
                        .filter(c -> c.getCityId().equals(req.cityId()))
                        .findFirst())
                .orElse(null);

        /* 2) — Fallback SECONDARY : par devise ——————————————— */
        if (cfg == null && req.currency() != null && !req.currency().isBlank()) {
            cfg = pricing.getCountry()
                    .values()
                    .stream()
                    .filter(c -> req.currency().equalsIgnoreCase(c.getCurrency()))
                    .findFirst()
                    .orElse(null);
            if (cfg != null) {
                log.debug("Tarif trouvé par devise fallback → {}", cfg.getCurrency());
            }
        }

        /* 3) — Extraction devise ———————————————————————————— */
        String currency = Optional.ofNullable(cfg)
                .map(PricingProperties.Country::getCurrency)
                .orElse(req.currency());

        /* 4) — Tarif distance ou forfait ———————————————— */
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

        double base = flatFare != null
                ? flatFare
                : req.distanceKm() * ratePerKm;

        /* 5) — Surge ———————————————————————————————————— */
        double surge = surgeService.getSurgeFactor(
                Optional.ofNullable(cfg).map(PricingProperties.Country::getCityId).orElse(req.cityId()),
                req.productType()
        );
        double total = base * surge;

        /* 6) — Commission ————————————————————————————— */
        double pct = Optional.ofNullable(cfg)
                .map(PricingProperties.Country::getPlatformShare)
                .map(m -> m.getOrDefault(req.productType().name(), m.getOrDefault("default", .20)))
                .orElse(0.20);

        log.debug("💰 quote cityId={} (cfg={}) type={} km={} base={}{} surge={}× total={} (platform {}%)",
                req.cityId(), cfg != null ? "ok" : "null",
                req.productType(), req.distanceKm(),
                money(base), currency, surge, money(total), pct * 100);

        /* 7) — Construction DTO ————————————————————————— */
        return mapper.toPriceQuoteDto(
                Optional.ofNullable(cfg).map(PricingProperties.Country::getCityId).orElse(req.cityId()),
                req.productType(),
                req.distanceKm(),
                Math.round(req.durationMin() * 60),
                money(base),
                money(surge),
                money(total),
                currency,
                OffsetDateTime.now().plusMinutes(5)
        );
    }
}
