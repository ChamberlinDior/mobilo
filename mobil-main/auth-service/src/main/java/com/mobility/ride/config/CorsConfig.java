package com.mobility.ride.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * -------------------------------------------------------------------------
 * CorsConfig – configuration globale CORS pour autoriser explicitement
 *              les appels depuis les domaines frontaux.
 *
 * • mappe toutes les routes commençant par /api/v1/…<br>
 * • autorise uniquement les origines frontales (à remplacer par vos domaines)<br>
 * • permet les méthodes HTTP les plus courantes et les en-têtes nécessaires<br>
 * • active le support des cookies (allowCredentials) si nécessaire<br>
 * • définit un TTL (maxAge) d’une heure pour la mise en cache des prévols CORS
 * -------------------------------------------------------------------------
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
                // Appliquer CORS à tous les endpoints REST (version v1)
                .addMapping("/api/v1/**")

                // Domains frontaux autorisés – remplacer par vos URLs
                .allowedOrigins(
                        "http://localhost:3000",
                        "https://app.example.com"
                )

                // Méthodes HTTP autorisées depuis le front
                .allowedMethods(
                        "GET",
                        "POST",
                        "PUT",
                        "PATCH",
                        "DELETE",
                        "OPTIONS"
                )

                // Autoriser tous les en-têtes (ou lister explicitement si vous préférez)
                .allowedHeaders("*")

                // Permettre l'envoi des cookies / Authorization header (si besoin)
                .allowCredentials(true)

                // Durée (en secondes) de mise en cache de la pré-vérification CORS
                .maxAge(3600);
    }
}
