/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/service/WalletService.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.service;

import com.mobility.auth.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * Opérations « wallet » communes aux riders & drivers.
 */
public interface WalletService {

    /** Recharge par carte / mobile money */
    WalletBalanceResponse topUp(String userUid, WalletTopUpRequest req);

    /** Solde courant */
    WalletBalanceResponse getBalance(String userUid);

    /** Historique paginé */
    Page<WalletTransactionResponse> listTransactions(String userUid, Pageable pageable);

    /** Retrait (driver) : virement du solde vers son compte bancaire */
    WalletBalanceResponse withdraw(String userUid, BigDecimal amount);
}
