// ───────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/GeoServiceImpl.java
//  v1.3 – RestTemplate + Google primaire, OSM fallback, cache 24 h
//         • locale configurable (language/accept-language)
//         • clé de cache robuste (Locale.ROOT)
//         • never-null (fallback "lat,lng")
//         • purge simple + limite taille cache
// ───────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GeoServiceImpl implements GeoService {

    private final RestTemplate restTemplate;

    /** Clé Google Geocoding (déjà présente dans application-*.yml). */
    @Value("${app.geocoding.api-key:}")
    private String googleApiKey;

    /** Fallback OSM (Nominatim). */
    @Value("${geo.reverse.url:https://nominatim.openstreetmap.org/reverse}")
    private String osmReverseUrl;

    /** Activer/désactiver le fallback OSM. */
    @Value("${geo.reverse.osm-fallback:true}")
    private boolean osmFallbackEnabled;

    /** Langue des adresses (ex: fr, en, fr-FR…). */
    @Value("${geo.reverse.locale:fr}")
    private String locale;

    /** Contact pour Nominatim (email/URL) – recommandé par la policy OSM. */
    @Value("${geo.reverse.contact:}")
    private String contact;

    /** Cache TTL 24 h (clé = "lat,lng"), limite souple pour éviter la dérive mémoire. */
    private static final long TTL_MS   = Duration.ofHours(24).toMillis();
    private static final int  MAX_SIZE = 5000;

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public GeoServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        // En-têtes par défaut (utile pour Nominatim)
        ClientHttpRequestInterceptor ua = (req, body, ex) -> {
            var h = req.getHeaders();
            h.addIfAbsent("User-Agent", "mobility-app/1.0 (reverse geocoder)");
            h.addIfAbsent("Accept", "application/json");
            // n'injecte pas Accept-Language si déjà présent
            if (!h.containsKey("Accept-Language")) {
                h.add("Accept-Language", "fr"); // sera surchargé par paramètre ?language=...
            }
            return ex.execute(req, body);
        };
        this.restTemplate.getInterceptors().add(ua);
    }

    @Override
    public String reverse(Double lat, Double lng) {
        if (lat == null || lng == null) return "Adresse inconnue";

        // clé de cache stable, insensible au locale (separateur '.')
        final String key = String.format(Locale.ROOT, "%.5f,%.5f", lat, lng);

        // 1) cache
        CacheEntry hit = cache.get(key);
        if (hit != null && !hit.isExpired()) return hit.address;

        String label = null;

        // 2) Google d’abord (plus précis/rapide)
        try {
            label = tryGoogle(lat, lng);
        } catch (Exception e) {
            log.warn("[GeoService] Google reverse({}) FAIL → {}", key, e.getMessage());
        }

        // 3) OSM fallback si nécessaire
        if (!StringUtils.hasText(label) && osmFallbackEnabled) {
            try {
                label = tryOsm(lat, lng);
            } catch (Exception e) {
                log.warn("[GeoService] OSM reverse({}) FAIL → {}", key, e.getMessage());
            }
        }

        // 4) fallback final : jamais null
        if (!StringUtils.hasText(label)) {
            label = key;
        }

        // 5) cache + petite purge
        cache.put(key, new CacheEntry(label));
        pruneCache();
        return label;
    }

    /* ───────────────────────── Internes ───────────────────────── */

    private String tryGoogle(double lat, double lng) throws RestClientException {
        if (!StringUtils.hasText(googleApiKey)) return null;

        String url = "https://maps.googleapis.com/maps/api/geocode/json"
                + "?latlng=" + lat + "," + lng
                + "&language=" + enc(locale)
                + "&result_type=street_address|route|premise"
                + "&key=" + enc(googleApiKey);

        GeocodeResponse resp = restTemplate.getForObject(url, GeocodeResponse.class);
        if (resp == null || resp.results == null || resp.results.isEmpty() || !"OK".equals(resp.status)) {
            return null; // ZERO_RESULTS / OVER_QUERY_LIMIT / REQUEST_DENIED → on laisse le fallback gérer
        }
        return resp.results.get(0).formattedAddress;
    }

    private String tryOsm(double lat, double lng) throws RestClientException {
        StringBuilder url = new StringBuilder(osmReverseUrl)
                .append("?format=json")
                .append("&lat=").append(lat)
                .append("&lon=").append(lng)
                .append("&zoom=18&addressdetails=0")
                .append("&accept-language=").append(enc(locale));
        if (StringUtils.hasText(contact)) {
            url.append("&email=").append(enc(contact)); // recommandé par la policy OSM
        }
        OsmResponse resp = restTemplate.getForObject(url.toString(), OsmResponse.class);
        return (resp != null && StringUtils.hasText(resp.displayName)) ? resp.displayName : null;
    }

    private static String enc(String s) {
        return URLEncoder.encode(s == null ? "" : s, StandardCharsets.UTF_8);
    }

    private void pruneCache() {
        if (cache.size() <= MAX_SIZE) return;
        // 1) jette les expirés
        cache.entrySet().removeIf(e -> e.getValue().isExpired());
        // 2) si encore trop gros, purge simple (fail-safe)
        if (cache.size() > MAX_SIZE) {
            log.debug("[GeoService] cache size={} > {} → clear()", cache.size(), MAX_SIZE);
            cache.clear();
        }
    }

    /* -------- DTO Google -------- */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class GeocodeResponse {
        public String status;
        public List<Result> results;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Result {
        @JsonProperty("formatted_address")
        public String formattedAddress;
    }

    /* -------- DTO OSM -------- */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class OsmResponse {
        @JsonProperty("display_name")
        public String displayName;
    }

    /* -------- Cache TTL -------- */
    private record CacheEntry(String address, long ts) {
        CacheEntry(String address) { this(address, System.currentTimeMillis()); }
        boolean isExpired() { return System.currentTimeMillis() - ts > TTL_MS; }
    }
}
