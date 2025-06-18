package com.mobility.auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration OpenAPI / Swagger pour l’Auth Service.
 *
 * • Déclare les méta-infos de l’API (titre, version, contact…).
 * • Configure le schéma de sécurité JWT Bearer, appliqué globalement.
 */
@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Méta-infos de l’API
                .info(new Info()
                        .title("Mobility Auth Service API")
                        .description("API d’authentification & gestion des utilisateurs pour Mobility")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Mobility API Team")
                                .email("support@mobility.example.com")
                                .url("https://mobility.example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html"))
                )
                // Documentation externe (facultatif)
                .externalDocs(new ExternalDocumentation()
                        .description("Guide d’utilisation")
                        .url("https://docs.mobility.example.com/auth-service"))
                // Composants de sécurité et autres schémas
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Token JWT à fournir dans le header Authorization: Bearer {token}"))
                )
                // Application globale de ce schéma à toutes les opérations
                .addSecurityItem(new SecurityRequirement()
                        .addList(SECURITY_SCHEME_NAME));
    }
}
