package com.mobility.auth.repository;

import com.mobility.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Accès base – Refresh Tokens (rotation & revocation).
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /* Récupération par valeur brute (UUID v4 string) */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Marque tous les refresh tokens actifs d’un utilisateur comme révoqués
     * (hard-revocation après changement de mot de passe, par ex.).
     */
    @Modifying
    @Transactional
    @Query("""
           update RefreshToken r
              set r.revoked = true
            where r.user.id = :userId
              and r.revoked = false
           """)
    void revokeAllForUser(@Param("userId") Long userId);
}
