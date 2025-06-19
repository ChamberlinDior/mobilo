// ============================================================================
//  PACKAGE : com.mobility.ride.service
//  FILE    : UpfrontPriceService.java               (v2025-07-01)
// ============================================================================
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

    private final SurgePricingService    surgeService;
    private final PricingMapper          mapper;
    private final PricingProperties      pricing;
    private final ExchangeRateService    exchangeRateService;

    private static final double DEFAULT_RATE_PER_KM = 1.0;

    private static BigDecimal money(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP);
    }

    public PriceQuoteResponse quote(@Valid PriceQuoteRequest req) {
        // --- deliveryZone et weight par défaut pour LOCAL ---
        DeliveryZone zone   = Optional.ofNullable(req.deliveryZone()).orElse(DeliveryZone.LOCAL);
        BigDecimal    weight = Optional.ofNullable(req.weightKg()).orElse(BigDecimal.ZERO);

        // si on n'est pas en LOCAL, le poids devient obligatoire
        if (zone != DeliveryZone.LOCAL) {
            Objects.requireNonNull(
                    req.weightKg(),
                    "Weight (kg) must be provided for interurban and international deliveries"
            );
        }

        // 1) Livraison INTERURBAIN / INTERNATIONAL
        if (zone != DeliveryZone.LOCAL) {
            BigDecimal perKg   = zone == DeliveryZone.INTERURBAIN
                    ? BigDecimal.valueOf(5000)
                    : BigDecimal.valueOf(10000);
            BigDecimal baseXaf = weight.multiply(perKg).setScale(2, RoundingMode.HALF_UP);

            String    outCurrency;
            BigDecimal totalFare;
            if (zone == DeliveryZone.INTERURBAIN) {
                outCurrency = "XAF";
                totalFare   = baseXaf;
            } else {
                // INTERNATIONAL
                outCurrency = req.deliveryZone() == DeliveryZone.INTERNATIONAL_USA ? "USD" : "EUR";
                BigDecimal rate = exchangeRateService.getRate("XAF", outCurrency);
                totalFare        = baseXaf.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            }

            BigDecimal surgeFactor = BigDecimal.ONE; // pas de surge sur livraisons

            return mapper.toPriceQuoteDto(
                    req.cityId(),
                    req.productType(),
                    req.distanceKm(),
                    Math.round(req.durationMin() * 60),
                    baseXaf,
                    surgeFactor,
                    totalFare,
                    outCurrency,
                    OffsetDateTime.now().plusMinutes(5),
                    weight,
                    zone
            );
        }

        // 2) Courses CLASSIQUES (LOCAL)
        PricingProperties.Country cfg = Optional.ofNullable(pricing.getCountry())
                .flatMap(map -> map.values().stream()
                        .filter(c -> c.getCityId().equals(req.cityId()))
                        .findFirst())
                .orElse(null);

        if (cfg == null && req.currency() != null && !req.currency().isBlank()) {
            cfg = pricing.getCountry().values().stream()
                    .filter(c -> req.currency().equalsIgnoreCase(c.getCurrency()))
                    .findFirst()
                    .orElse(null);
            if (cfg != null) log.debug("Tarif fallback par devise → {}", cfg.getCurrency());
        }

        String currency = Optional.ofNullable(cfg)
                .map(PricingProperties.Country::getCurrency)
                .orElse(req.currency());

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
                Optional.ofNullable(cfg)
                        .map(PricingProperties.Country::getCityId)
                        .orElse(req.cityId()),
                req.productType()
        );
        double total = base * surge;

        log.debug("💰 quote cityId={} type={} km={} base={}{} surge={}× total={}",
                req.cityId(), req.productType(),
                req.distanceKm(), money(base), currency,
                money(surge), money(total)
        );

        return mapper.toPriceQuoteDto(
                Optional.ofNullable(cfg)
                        .map(PricingProperties.Country::getCityId)
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
