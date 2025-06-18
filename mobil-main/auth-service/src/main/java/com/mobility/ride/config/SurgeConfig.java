package com.mobility.ride.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

/**
 * <h2>SurgeConfig – configuration du cache “surge”</h2>
 *
 * <p>
 *   • Deux caches distincts, chacun avec un nom unique :
 *     – <strong>surge::info</strong>   – pour les objets de type {@link com.mobility.ride.dto.SurgeInfoResponse}
 *     – <strong>surge::factor</strong> – pour les valeurs de type <code>double</code> (facteur brut)
 *   • TTL = 30 s (expireAfterWrite)
 *   • Taille max : 2 000 entrées (≈ 500 villes × 4 produits)
 * </p>
 *
 * <pre>
 *     |---- clé -----------------------| |----- valeur -----| |-- TTL --|
 *     surge::info:123-POOL            → SurgeInfoResponse    30 s
 *     surge::factor:123-POOL          → Double               30 s
 * </pre>
 */
@Configuration
@EnableCaching
public class SurgeConfig {

    // Nom unique pour le cache “info”
    private static final String CACHE_INFO   = "surge::info";

    // Nom unique pour le cache “factor”
    private static final String CACHE_FACTOR = "surge::factor";

    /**
     * Spécification Caffeine commune : expiration après écriture (30 s)
     * et taille maximale de 2 000 entrées.
     */
    private Caffeine<Object, Object> caffeineSpec() {
        return Caffeine.newBuilder()
                .maximumSize(2_000)
                .expireAfterWrite(Duration.ofSeconds(30));
    }

    /**
     * Définit le CacheManager avec les deux caches “surge::info” et “surge::factor”.
     * Chacun utilise la même configuration Caffeine (TTL = 30 s, max = 2 000).
     */
    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager cm = new SimpleCacheManager();
        cm.setCaches(List.of(
                new CaffeineCache(CACHE_INFO,   caffeineSpec().build()),
                new CaffeineCache(CACHE_FACTOR, caffeineSpec().build())
        ));
        return cm;
    }
}
