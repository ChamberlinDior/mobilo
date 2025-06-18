package com.mobility.auth.config;

import com.mobility.auth.security.MyAuthenticationProvider;
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
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;      // ⬅️ bon package
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;

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
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg)
            throws Exception {
        return cfg.getAuthenticationManager();
    }

    /* ─────────── Chaîne de filtres Spring-Security ─────────── */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   MyAuthenticationProvider myAuthProvider)
            throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(myAuthProvider)                       // login / signup
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults())) // JWT Bearer
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /* ─────────── Décodage / validation JWT (HS256) ─────────── */
    @Bean
    public JwtDecoder jwtDecoder(@Value("${app.jwt.secret}") String secretB64) {

        // Clé Base64 d’au moins 256 bits (32 octets)
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretB64));

        return NimbusJwtDecoder
                .withSecretKey(key)
                .macAlgorithm(MacAlgorithm.HS256)   // explicite & conforme
                .build();
    }
}
