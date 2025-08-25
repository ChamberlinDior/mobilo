package com.mobility.ride.service;

import com.mobility.ride.config.PricingProperties;
import com.mobility.ride.geo.CityInfo;
import com.mobility.ride.geo.GeoLocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrencyResolverImpl implements CurrencyResolver {

    private final GeoLocationService geoLocationService; // déjà utilisé par PricingController
    private final PricingProperties  pricing;

    @Override
    public String resolve(Double lat, Double lng, Long fallbackCityId) {
        try {
            if (lat != null && lng != null) {
                CityInfo info = geoLocationService.resolve(new com.mobility.ride.model.LatLng(lat, lng));
                if (info != null && info.currency() != null && !info.currency().isBlank()) return info.currency();
                if (info != null && info.cityId() != null) return currencyFromCityId(info.cityId());
            }
        } catch (Exception ignore) { /* fallback */ }

        if (fallbackCityId != null) return currencyFromCityId(fallbackCityId);

        // dernier recours : pricing.country.default.currency (ou USD)
        return Optional.ofNullable(pricing.getCountry())
                .map(m -> m.get("default"))
                .map(PricingProperties.Country::getCurrency)
                .orElse("USD");
    }

    private String currencyFromCityId(Long cityId) {
        return Optional.ofNullable(pricing.getCountry())
                .flatMap(map -> map.values().stream()
                        .filter(c -> c.getCityId().equals(cityId))
                        .findFirst())
                .map(PricingProperties.Country::getCurrency)
                .orElseGet(() -> Optional.ofNullable(pricing.getCountry())
                        .map(m -> m.get("default"))
                        .map(PricingProperties.Country::getCurrency)
                        .orElse("USD"));
    }
}
