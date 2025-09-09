// ─────────────────────────────────────────────────────────────
// FILE : auth-service/src/main/java/com/mobility/auth/config/SecurityConfig.java
// v2025-10-06 – JWT HS256, CORS, préflight, handlers 401/403, method security
// ─────────────────────────────────────────────────────────────
package com.mobility.auth.config;

import com.mobility.auth.security.MyAuthenticationProvider;
import com.mobility.auth.ws.JwtHandshakeInterceptor;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import javax.crypto.SecretKey;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // ← si tu veux utiliser @PreAuthorize sur tes services/controllers
public class SecurityConfig {

    /* ─────────── Encodage mots de passe ─────────── */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /* ─────────── AuthenticationManager global ─────────── */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    /* ─────────── Chaîne de filtres Spring-Security ─────────── */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   MyAuthenticationProvider myAuthProvider) throws Exception {

        return http
                /* CORS ; si un CorsConfigurationSource existe, il sera utilisé */
                .cors(Customizer.withDefaults())

                /* REST stateless + WebSocket : CSRF ignoré sur /ws/** */
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/ws/**"))
                        .disable())

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(myAuthProvider)

                /* Ressource server JWT (Authorization: Bearer …) */
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

                /* Erreurs propres JSON pour JWT manquant/mauvais (401) et refus (403) */
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint())
                        .accessDeniedHandler(new BearerTokenAccessDeniedHandler())
                )

                .authorizeHttpRequests(auth -> auth
                        /* Pré-flight CORS */
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        /* Public : auth, docs, swagger, WS handshake, health */
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/ws/**",
                                "/actuator/health"
                        ).permitAll()

                        /* Tout le reste → JWT requis */
                        .anyRequest().authenticated()
                )
                .build();
    }

    /* ─────────── Décodage / validation JWT (HS256) ─────────── */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secretB64) {
        // secretB64 doit être en Base64 (ex: sCyaX+...=)
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretB64));
        return NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    /* ─────────── Intercepteur WebSocket JWT ─────────── */
    @Bean
    public JwtHandshakeInterceptor jwtHandshakeInterceptor(JwtDecoder decoder) {
        return new JwtHandshakeInterceptor(decoder);
    }
}
