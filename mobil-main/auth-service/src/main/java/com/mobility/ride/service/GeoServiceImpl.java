// ───────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/GeoServiceImpl.java
//  v1.1 – WebClient + cache 24 h, même package que GeoService
// ───────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GeoServiceImpl implements GeoService {

    /* URL Nominatim par défaut (configurable via application.yml/properties) */
    @Value("${geo.reverse.url:https://nominatim.openstreetmap.org/reverse}")
    private String reverseUrl;

    private WebClient webClient;

    /* Cache TTL 24 h  —  clé : "lat,lng"  ➜ adresse */
    private static final long TTL = Duration.ofHours(24).toMillis();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        webClient = WebClient.builder()
                .baseUrl(reverseUrl)
                .defaultHeader("User-Agent", "mobility-app/1.0 (reverse geocoder)")
                .build();
    }

    @Override
    public String reverse(Double lat, Double lng) {

        if (lat == null || lng == null) return "Adresse inconnue";
        String key = String.format("%.5f,%.5f", lat, lng);

        /* Hit cache ? */
        CacheEntry hit = cache.get(key);
        if (hit != null && !hit.isExpired()) return hit.address;

        /* Appel HTTP */
        try {
            String label = webClient.get()
                    .uri(uri -> uri.queryParam("format", "json")
                            .queryParam("lat", lat)
                            .queryParam("lon", lng)
                            .queryParam("zoom", 18)
                            .build())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, resp -> resp.createException())
                    .bodyToMono(JsonNode.class)
                    .map(j -> j.path("display_name").asText(key))
                    .timeout(Duration.ofSeconds(4))
                    .onErrorResume(err -> {
                        log.warn("[GeoService] reverse({}) FAIL → {}", key, err.getMessage());
                        return Mono.just(key);
                    })
                    .block();

            cache.put(key, new CacheEntry(label));
            return label;

        } catch (Exception ex) {
            log.warn("[GeoService] reverse({}) EX → {}", key, ex.getMessage());
            return key;  // fallback en cas d’échec
        }
    }

    /* -------- interne : petite entrée de cache TTL -------- */
    private record CacheEntry(String address, long ts) {
        CacheEntry(String address) { this(address, System.currentTimeMillis()); }
        boolean isExpired() { return System.currentTimeMillis() - ts > TTL; }
    }
}
