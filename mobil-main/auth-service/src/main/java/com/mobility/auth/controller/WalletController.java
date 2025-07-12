/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/controller/WalletController.java
 * v2025-10-06  – endpoint /withdraw corrigé
 * ------------------------------------------------------------------*/
package com.mobility.auth.controller;

import com.mobility.auth.dto.*;
import com.mobility.auth.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletSvc;

    /* ───────── Solde ───────── */
    @GetMapping("/balance")
    public WalletBalanceResponse balance(@AuthenticationPrincipal Jwt jwt) {
        return walletSvc.getBalance(jwt.getSubject());
    }

    /* ───────── Recharge ────── */
    @PostMapping("/top-up")
    public WalletBalanceResponse topUp(@AuthenticationPrincipal Jwt jwt,
                                       @RequestBody @Valid WalletTopUpRequest req) {
        return walletSvc.topUp(jwt.getSubject(), req);
    }

    /* ───────── Historique ──── */
    @GetMapping("/transactions")
    public Page<WalletTransactionResponse> txns(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC) Pageable p) {
        return walletSvc.listTransactions(jwt.getSubject(), p);
    }

    /* ───────── Retrait driver ─ */
    @PostMapping("/withdraw")
    public WalletBalanceResponse withdraw(@AuthenticationPrincipal Jwt jwt,
                                          @RequestBody @Valid WalletWithdrawRequest req) {

        // Payload minimal : { "amount": 123.45 }
        return walletSvc.withdraw(
                jwt.getSubject(),
                req.amount()               // <-- plus de valueOf()
        );
    }
}
