/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/service/WalletService.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.service;

import com.mobility.auth.dto.WalletBalanceResponse;
import com.mobility.auth.dto.WalletTopUpRequest;
import com.mobility.auth.dto.WalletTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Opérations « wallet » communes aux riders & drivers.
 * <p>
 * Idempotence :
 * <ul>
 *   <li>Pour chaque opération mutative (top-up, withdraw), un en-tête
 *       <code>Idempotency-Key</code> peut être transmis jusqu’au service pour
 *       éviter les doublons (retries réseau, double-tap, etc.).</li>
 *   <li>La clé peut être {@code null} : dans ce cas, l’implémentation peut
 *       en générer une ou traiter sans idempotence forte.</li>
 * </ul>
 * <p>
 * Rétro-compatibilité :
 * <ul>
 *   <li>Des méthodes par défaut (overloads) sans clé sont fournies et délèguent
 *       aux variantes avec clé en passant {@code null}.</li>
 * </ul>
 */
public interface WalletService {

    /* =========================================================
       Rechargement (TOP-UP)
       ========================================================= */

    /**
     * Recharge le wallet de l’utilisateur.
     *
     * @param userUid          identifiant fonctionnel (JWT sub / externalUid)
     * @param req              montant, devise (optionnelle), moyen de paiement
     * @param idempotencyKey   clé d’idempotence (nullable)
     * @return nouveau solde (wallet/promo/credit)
     */
    WalletBalanceResponse topUp(String userUid, WalletTopUpRequest req, String idempotencyKey);

    /** Overload rétro-compatible sans clé d’idempotence. */
    default WalletBalanceResponse topUp(String userUid, WalletTopUpRequest req) {
        return topUp(userUid, req, null);
    }

    /* =========================================================
       Solde & Historique
       ========================================================= */

    /** Solde courant (wallet/promo/credit). */
    WalletBalanceResponse getBalance(String userUid);

    /** Historique paginé le plus récent en premier. */
    Page<WalletTransactionResponse> listTransactions(String userUid, Pageable pageable);

    /* =========================================================
       Retrait (WITHDRAW)
       ========================================================= */

    /**
     * Retrait du solde (payout) vers le compte bancaire / mobile money.
     *
     * @param userUid          identifiant fonctionnel
     * @param amount           montant à débiter (devise du wallet)
     * @param idempotencyKey   clé d’idempotence (nullable)
     * @return solde après retrait
     */
    WalletBalanceResponse withdraw(String userUid, BigDecimal amount, String idempotencyKey);

    /** Overload rétro-compatible sans clé d’idempotence. */
    default WalletBalanceResponse withdraw(String userUid, BigDecimal amount) {
        return withdraw(userUid, amount, null);
    }
}
