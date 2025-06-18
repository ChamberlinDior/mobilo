/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/repository/WalletTransactionRepository.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.repository;

import com.mobility.auth.model.WalletTransaction;
import com.mobility.auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {

    /**
     * Renvoie l’historique des transactions d’un utilisateur,
     * trié du plus récent au plus ancien.
     *
     * @param user     l’entité User
     * @param pageable pagination / tri Spring Data
     * @return page de WalletTransaction
     */
    Page<WalletTransaction> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
}
