// ─────────────────────────────────────────────────────────────
// FILE : auth-service/src/main/java/com/mobility/auth/config/SecurityConfig.java
// v2025-08-26 – config unique (auth + ride), CORS activé, préflight OPTIONS permis
// ─────────────────────────────────────────────────────────────
package com.mobility.auth.config;

import com.mobility.auth.security.MyAuthenticationProvider;
import com.mobility.auth.ws.JwtHandshakeInterceptor;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.http.HttpMethod;

import javax.crypto.SecretKey;

@Configuration
@EnableWebSecurity
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
                /* Active CORS ; si un bean CorsConfigurationSource existe (ex: CorsConfig), il sera utilisé */
                .cors(Customizer.withDefaults())

                /* WebSocket : handshake GET /ws/** autorisé, CSRF ignoré (REST stateless) */
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(new AntPathRequestMatcher("/ws/**"))
                        .disable())

                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(myAuthProvider)

                /* Ressource server JWT (Authorization: Bearer …) */
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

                .authorizeHttpRequests(auth -> auth
                        /* Pré-flight CORS */
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        /* Public : auth, docs, swagger, handshake WS */
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/ws/**"
                        ).permitAll()

                        /* Le reste nécessite un JWT valide */
                        .anyRequest().authenticated()
                )
                .build();
    }

    /* ─────────── Décodage / validation JWT (HS256) ─────────── */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secretB64) {
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
