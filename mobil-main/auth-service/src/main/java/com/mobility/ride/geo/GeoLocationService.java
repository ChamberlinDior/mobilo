// ============================================================================
//  FILE    : GeoLocationService.java
//  PACKAGE : com.mobility.ride.geo
// ----------------------------------------------------------------------------
package com.mobility.ride.geo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mobility.ride.config.PricingProperties;
import com.mobility.ride.model.LatLng;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service de géolocalisation inverse :
 * - interroge l'API Google Geocoding pour obtenir le country code d'un LatLng
 * - traduit ce code en clé (« gabon », « france », « usa »)
 * - renvoie un CityInfo(cityId, currency) d'après PricingProperties
 */
@Service
public class GeoLocationService {

    private final PricingProperties pricingProperties;
    private final RestTemplate       restTemplate;
    private final String             googleApiKey;

    public GeoLocationService(
            PricingProperties pricingProperties,
            RestTemplate restTemplate,
            @Value("${app.geocoding.api-key}") String googleApiKey
    ) {
        this.pricingProperties = pricingProperties;
        this.restTemplate      = restTemplate;
        this.googleApiKey      = googleApiKey;
    }

    /**
     * Résout un CityInfo (cityId + currency) pour un LatLng donné.
     */
    public CityInfo resolve(LatLng latLng) {
        // 1) Appel à Google Geocoding API
        String url = "https://maps.googleapis.com/maps/api/geocode/json" +
                "?latlng=" + latLng.getLat() + "," + latLng.getLng() +
                "&key=" + googleApiKey;
        GeocodeResponse resp = restTemplate.getForObject(url, GeocodeResponse.class);

        if (resp == null || !"OK".equals(resp.status)) {
            throw new RuntimeException("Erreur géocodage : " + (resp == null ? "null" : resp.status));
        }

        // 2) Extraction du code pays (ex. « GA », « FR », « US »)
        String countryCode = resp.results.stream()
                .flatMap(r -> r.addressComponents.stream())
                .filter(ac -> ac.types.contains("country"))
                .findFirst()
                .map(ac -> ac.shortName)
                .orElseThrow(() -> new RuntimeException("Country code non trouvé"));

        // 3) Mapping vers votre clé pricing
        String key = switch (countryCode) {
            case "GA" -> "gabon";
            case "FR" -> "france";
            case "US" -> "usa";
            default   -> throw new RuntimeException("Pays non supporté : " + countryCode);
        };

        // 4) Récupération de la config tarifaire
        Map<String, PricingProperties.Country> map = pricingProperties.getCountry();
        PricingProperties.Country cfg = Optional.ofNullable(map.get(key))
                .orElseThrow(() ->
                        new RuntimeException("Configuration tarifaire manquante pour « " + key + " »")
                );

        return new CityInfo(cfg.getCityId(), cfg.getCurrency());
    }

    // ───────────────────────────
    // Classes internes pour désérialiser la réponse JSON de Google
    // ───────────────────────────

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class GeocodeResponse {
        String status;
        List<Result> results;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Result {
        @JsonProperty("address_components")
        List<AddressComponent> addressComponents;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AddressComponent {
        @JsonProperty("short_name")
        String shortName;
        List<String> types;
    }
}
