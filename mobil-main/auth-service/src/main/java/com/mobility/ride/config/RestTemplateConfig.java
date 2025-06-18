// src/main/java/com/mobility/ride/config/RestTemplateConfig.java
package com.mobility.ride.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * Bean RestTemplate pour faire des appels HTTP (ici vers Google Geocoding).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
