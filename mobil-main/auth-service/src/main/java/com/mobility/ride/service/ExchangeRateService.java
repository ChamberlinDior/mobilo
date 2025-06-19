// src/main/java/com/mobility/ride/service/ExchangeRateService.java
package com.mobility.ride.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Service de conversion de devises.
 * Interroge une API externe pour récupérer les taux XAF→USD/EUR.
 */
@Service
public class ExchangeRateService {

    private final RestTemplate restTemplate;
    private final String apiUrl;
    private final String apiKey;

    public ExchangeRateService(
            @Value("${exchange.api.url}") String apiUrl,
            @Value("${exchange.api.key}") String apiKey
    ) {
        this.restTemplate = new RestTemplate();
        this.apiUrl       = apiUrl;
        this.apiKey       = apiKey;
    }

    /**
     * Récupère le taux de change pour convertir une unité de {@code from}
     * en {@code to}.
     *
     * @param from devise source (ex. "XAF")
     * @param to   devise cible (ex. "USD" ou "EUR")
     * @return taux de change
     */
    public BigDecimal getRate(String from, String to) {
        // Le endpoint doit ressembler à :
        // https://api.exchangerate.host/latest?access_key=API_KEY&base=XAF&symbols=USD
        String url = String.format("%s?access_key=%s&base=%s&symbols=%s",
                apiUrl, apiKey, from, to);

        ExchangeRateResponse resp = restTemplate
                .getForObject(url, ExchangeRateResponse.class);

        if (resp == null || !resp.isSuccess() || resp.getRates() == null) {
            throw new IllegalStateException(
                    "Impossible de récupérer le taux de change pour " + from + "→" + to
            );
        }
        BigDecimal rate = resp.getRates().get(to);
        if (rate == null) {
            throw new IllegalArgumentException(
                    "Taux introuvable pour la devise cible : " + to
            );
        }
        return rate;
    }

    /**
     * Modélisation de la réponse de l’API de change.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ExchangeRateResponse {
        private boolean success;

        @JsonProperty("rates")
        private Map<String, BigDecimal> rates;

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public Map<String, BigDecimal> getRates() { return rates; }
        public void setRates(Map<String, BigDecimal> rates) { this.rates = rates; }
    }
}
