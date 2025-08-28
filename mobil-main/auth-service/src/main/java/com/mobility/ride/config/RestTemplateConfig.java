package com.mobility.ride.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * RestTemplate centralisé :
 *  - timeouts courts (fail fast) pour déclencher le fallback OSM
 *  - headers par défaut (User-Agent, Accept)
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Timeouts "simples" (ms) – efficaces et sans dépendances externes
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(800);  // 0.8s
        f.setReadTimeout(800);     // 0.8s

        RestTemplate rt = new RestTemplate(f);

        // Petits headers par défaut (utile pour Nominatim + JSON)
        rt.setInterceptors(List.of((req, body, ex) -> {
            HttpHeaders h = req.getHeaders();
            h.addIfAbsent("User-Agent", "mobility-app/1.0 (resttemplate)");
            h.addIfAbsent("Accept", "application/json");
            return ex.execute(req, body);
        }));

        return rt;
    }
}
