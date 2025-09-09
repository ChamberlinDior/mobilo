/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/repository/WalletTransactionRepository.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.repository;

import com.mobility.auth.model.User;
import com.mobility.auth.model.WalletTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Accès lecture/écriture aux transactions wallet.
 */
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * Historique paginé d’un utilisateur, du plus récent au plus ancien.
     */
    Page<WalletTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    /**
     * Recherche d’une transaction existante par clé d’idempotence (anti double-débit).
     */
    Optional<WalletTransaction> findByUserAndIdempotencyKey(User user, String idempotencyKey);
}
