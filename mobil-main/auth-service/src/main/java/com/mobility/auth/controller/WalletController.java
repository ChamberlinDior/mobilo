/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/controller/WalletController.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.controller;

import com.mobility.auth.dto.WalletBalanceResponse;
import com.mobility.auth.dto.WalletTopUpRequest;
import com.mobility.auth.dto.WalletTransactionResponse;
import com.mobility.auth.dto.WalletWithdrawRequest;
import com.mobility.auth.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class WalletController {

    public static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final WalletService walletSvc;

    /* ───────── Solde ───────── */
    @GetMapping("/balance")
    public WalletBalanceResponse balance(@AuthenticationPrincipal Jwt jwt) {
        return walletSvc.getBalance(jwt.getSubject());
    }

    /* ───────── Recharge (TOP-UP) ────── */
    @PostMapping("/top-up")
    public WalletBalanceResponse topUp(@AuthenticationPrincipal Jwt jwt,
                                       @RequestBody @Valid WalletTopUpRequest req,
                                       @RequestHeader(name = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            log.debug("Idempotency-Key reçu pour TOP-UP uid={} : {}", jwt.getSubject(), idempotencyKey);
        }
        // Passe la clé d'idempotence au service
        return walletSvc.topUp(jwt.getSubject(), req, idempotencyKey);
    }

    /* ───────── Historique (paginé) ──── */
    @GetMapping("/transactions")
    public Page<WalletTransactionResponse> transactions(@AuthenticationPrincipal Jwt jwt,
                                                        @PageableDefault(size = 20,
                                                                sort = "createdAt",
                                                                direction = Sort.Direction.DESC)
                                                        Pageable pageable) {
        return walletSvc.listTransactions(jwt.getSubject(), pageable);
    }

    /* ───────── Retrait (WITHDRAW) ───── */
    @PostMapping("/withdraw")
    public WalletBalanceResponse withdraw(@AuthenticationPrincipal Jwt jwt,
                                          @RequestBody @Valid WalletWithdrawRequest req,
                                          @RequestHeader(name = IDEMPOTENCY_HEADER, required = false) String idempotencyKey) {

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            log.debug("Idempotency-Key reçu pour WITHDRAW uid={} : {}", jwt.getSubject(), idempotencyKey);
        }
        // Passe la clé d'idempotence au service
        return walletSvc.withdraw(jwt.getSubject(), req.amount(), idempotencyKey);
    }
}
