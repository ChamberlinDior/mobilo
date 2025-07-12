/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/service/WalletServiceImpl.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.service;

import com.mobility.auth.dto.*;
import com.mobility.auth.model.User;
import com.mobility.auth.model.WalletTransaction;
import com.mobility.auth.model.enums.WalletTxnType;
import com.mobility.auth.repository.UserRepository;
import com.mobility.auth.repository.WalletTransactionRepository;
import com.mobility.ride.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserRepository              userRepo;
    private final WalletTransactionRepository txnRepo;
    private final PaymentService              paymentService;
    private final PaymentMethodService        paymentMethodSvc;

    /* =============================================================
       1) Rechargement – TOP_UP
       ============================================================*/
    @Override
    @Transactional
    public WalletBalanceResponse topUp(String uid, WalletTopUpRequest req) {

        User user = userRepo.findByExternalUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        // 1. Vérifier la propriété du moyen de paiement
        paymentMethodSvc.setDefault(uid, Long.parseLong(req.getPaymentMethodId()));

        // 2. Débit PSP
        paymentService.authorizeRide(null, req.getAmount(), req.getCurrency());
        String chargeRef = "CHARGE-" + System.currentTimeMillis();   // stub

        // 3. Créditer le solde
        BigDecimal newBalance = user.getWalletBalance().add(req.getAmount());
        user.setWalletBalance(newBalance);
        userRepo.save(user);

        // 4. Historiser la transaction
        txnRepo.save(WalletTransaction.builder()
                .user     (user)
                .type     (WalletTxnType.TOP_UP)
                .amount   (req.getAmount())
                .currency (req.getCurrency())
                .reference(chargeRef)
                .build());

        return new WalletBalanceResponse(
                newBalance,
                user.getPromoBalance(),
                user.getCreditBalance());
    }

    /* =============================================================
       2) Consultation solde
       ============================================================*/
    @Override
    public WalletBalanceResponse getBalance(String uid) {
        User u = userRepo.findByExternalUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        return new WalletBalanceResponse(
                u.getWalletBalance(),
                u.getPromoBalance(),
                u.getCreditBalance());
    }

    /* =============================================================
       3) Historique paginé
       ============================================================*/
    @Override
    public Page<WalletTransactionResponse> listTransactions(String uid, Pageable pageable) {
        User u = userRepo.findByExternalUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        return txnRepo.findByUserOrderByCreatedAtDesc(u, pageable)
                .map(t -> WalletTransactionResponse.builder()
                        .id       (t.getId())
                        .type     (t.getType())
                        .amount   (t.getAmount())
                        .currency (t.getCurrency())
                        .reference(t.getReference())
                        .createdAt(t.getCreatedAt())
                        .build());
    }

    /* =============================================================
       4) Retrait (driver) – crédite un débit WITHDRAWAL
       ============================================================*/
    @Override
    @Transactional
    public WalletBalanceResponse withdraw(String uid, BigDecimal amount) {

        User driver = userRepo.findByExternalUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (driver.getWalletBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("INSUFFICIENT_FUNDS");
        }

        // appel PSP / virement SEPA en pratique …
        paymentService.transferToBank(driver, amount, driver.getDefaultCurrency());

        BigDecimal newBal = driver.getWalletBalance().subtract(amount);
        driver.setWalletBalance(newBal);
        userRepo.save(driver);

        txnRepo.save(WalletTransaction.builder()
                .user     (driver)
                .type     (WalletTxnType.WITHDRAWAL)
                .amount   (amount.negate())
                .currency (driver.getDefaultCurrency())
                .reference("PAYOUT-" + System.currentTimeMillis())
                .build());

        return new WalletBalanceResponse(newBal,
                driver.getPromoBalance(),
                driver.getCreditBalance());
    }
}
