/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/service/WalletService.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.service;

import com.mobility.auth.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WalletService {
    WalletBalanceResponse topUp(String userUid, WalletTopUpRequest req);
    WalletBalanceResponse getBalance(String userUid);
    Page<WalletTransactionResponse> listTransactions(String userUid, Pageable pageable);
}
