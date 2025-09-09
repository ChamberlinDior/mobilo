/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/service/WalletServiceImpl.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.service;

import com.mobility.auth.dto.WalletBalanceResponse;
import com.mobility.auth.dto.WalletTopUpRequest;
import com.mobility.auth.dto.WalletTransactionResponse;
import com.mobility.auth.model.User;
import com.mobility.auth.model.WalletTransaction;
import com.mobility.auth.model.enums.WalletTxnType;
import com.mobility.auth.repository.UserRepository;
import com.mobility.auth.repository.WalletTransactionRepository;
import com.mobility.ride.service.ExchangeRateService;
import com.mobility.ride.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final UserRepository              userRepo;
    private final WalletTransactionRepository txnRepo;
    private final PaymentService              paymentService;
    private final PaymentMethodService        paymentMethodSvc;
    private final ExchangeRateService         fxService;   // expose getRate(from,to) et providerName()

    /* ======================= Helpers ======================= */

    /** Devise effective du WALLET (défaut utilisateur → fallback USD). */
    private String resolveWalletCurrency(User u) {
        if (StringUtils.isNotBlank(u.getDefaultCurrency())) return u.getDefaultCurrency();
        return "USD"; // fallback si non renseignée
    }

    /** Mise à l’échelle décimale par devise (CFA sans décimales). */
    private BigDecimal scale(String currency, BigDecimal amount) {
        int scale = "XAF".equalsIgnoreCase(currency) ? 0 : 2;
        return amount.setScale(scale, RoundingMode.HALF_UP);
    }

    /** Parse sécurisé pour paymentMethodId éventuel (String → Long). */
    private Long safeLong(String s) {
        try { return StringUtils.isBlank(s) ? null : Long.valueOf(s.trim()); }
        catch (NumberFormatException nfe) { return null; }
    }

    /* ======================= API ======================= */

    @Override
    @Transactional
    public WalletBalanceResponse topUp(String uid, WalletTopUpRequest req, String idempotencyKey) {

        // 0) Validation basique
        if (req == null || req.getAmount() == null || req.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }

        // 1) Charge + lock pessimiste si dispo
        User user = userRepo.findByExternalUidForUpdate(uid)
                .orElseGet(() -> userRepo.findByExternalUid(uid)
                        .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND")));

        // 2) Idempotency (replay-safe)
        final String idemKey = StringUtils.trimToNull(idempotencyKey);
        if (idemKey != null) {
            Optional<WalletTransaction> existing = txnRepo.findByUserAndIdempotencyKey(user, idemKey);
            if (existing.isPresent()) {
                // transaction déjà comptabilisée → renvoie le solde actuel
                return new WalletBalanceResponse(
                        Optional.ofNullable(user.getWalletBalance()).orElse(BigDecimal.ZERO),
                        Optional.ofNullable(user.getPromoBalance()).orElse(BigDecimal.ZERO),
                        Optional.ofNullable(user.getCreditBalance()).orElse(BigDecimal.ZERO)
                );
            }
        }

        // 3) Devise du wallet et devise d’entrée (payload optionnel)
        final String walletCcy = resolveWalletCurrency(user);
        final String inputCcy  = StringUtils.isNotBlank(req.getCurrency())
                ? req.getCurrency().toUpperCase()
                : walletCcy;

        // 4) Moyens de paiement (positionner le défaut si fourni)
        Long methodId = safeLong(req.getPaymentMethodId());
        if (methodId != null) {
            try {
                paymentMethodSvc.setDefault(uid, methodId);
            } catch (Exception e) {
                log.warn("Impossible de définir le moyen par défaut (methodId={}): {}", methodId, e.getMessage());
            }
        }

        // 5) Autorisation PSP dédiée au TOP-UP (plus de authorizeRide(null,...))
        String providerRef = null;
        try {
            providerRef = paymentService.authorizeWalletTopUp(
                    user,
                    req.getAmount(),
                    inputCcy,
                    methodId,
                    idemKey
            );
        } catch (Throwable t) {
            log.warn("authorizeWalletTopUp a échoué, fallback ref synthétique. cause={}", t.toString());
        }
        if (StringUtils.isBlank(providerRef)) {
            providerRef = "TOPUP-" + System.currentTimeMillis();
        }

        // 6) Conversion FX si nécessaire (ExchangeRateService -> getRate / providerName)
        BigDecimal credited = req.getAmount();
        BigDecimal fxRate   = BigDecimal.ONE;
        String     fxProv   = null;

        try {
            if (!inputCcy.equalsIgnoreCase(walletCcy)) {
                fxRate   = fxService.getRate(inputCcy, walletCcy);
                credited = req.getAmount().multiply(fxRate);
                fxProv   = fxService.providerName();
            }
        } catch (Throwable ignored) {
            fxRate   = BigDecimal.ONE;
            credited = req.getAmount();
        }

        credited = scale(walletCcy, credited);

        // 7) MAJ du solde (init à 0 si null) + fixe la devise si absente
        BigDecimal current    = Optional.ofNullable(user.getWalletBalance()).orElse(BigDecimal.ZERO);
        BigDecimal newBalance = scale(walletCcy, current.add(credited));
        user.setWalletBalance(newBalance);
        if (StringUtils.isBlank(user.getDefaultCurrency())) {
            user.setDefaultCurrency(walletCcy);
        }
        userRepo.save(user);

        // 8) Historisation (+ trace FX) + idempotencyKey si présent
        txnRepo.save(WalletTransaction.builder()
                .user(user)
                .type(WalletTxnType.TOP_UP)
                .amount(credited)               // EN DEVISE DU WALLET
                .currency(walletCcy)
                .reference(providerRef)
                .idempotencyKey(idemKey)
                .amountOriginal(req.getAmount())
                .currencyOriginal(inputCcy)
                .fxRate(fxRate)
                .fxProvider(fxProv)
                .build());

        return new WalletBalanceResponse(newBalance,
                Optional.ofNullable(user.getPromoBalance()).orElse(BigDecimal.ZERO),
                Optional.ofNullable(user.getCreditBalance()).orElse(BigDecimal.ZERO));
    }

    @Override
    public WalletBalanceResponse getBalance(String uid) {
        User u = userRepo.findByExternalUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        return new WalletBalanceResponse(
                Optional.ofNullable(u.getWalletBalance()).orElse(BigDecimal.ZERO),
                Optional.ofNullable(u.getPromoBalance()).orElse(BigDecimal.ZERO),
                Optional.ofNullable(u.getCreditBalance()).orElse(BigDecimal.ZERO)
        );
    }

    @Override
    public Page<WalletTransactionResponse> listTransactions(String uid, Pageable pageable) {
        User u = userRepo.findByExternalUid(uid)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        return txnRepo.findByUserOrderByCreatedAtDesc(u, pageable)
                .map(t -> WalletTransactionResponse.builder()
                        .id(t.getId())
                        .type(t.getType())
                        .amount(t.getAmount())
                        .currency(t.getCurrency())
                        .reference(t.getReference())
                        .createdAt(t.getCreatedAt())
                        .amountOriginal(t.getAmountOriginal())
                        .currencyOriginal(t.getCurrencyOriginal())
                        .fxRate(t.getFxRate())
                        .fxProvider(t.getFxProvider())
                        .build());
    }

    @Override
    @Transactional
    public WalletBalanceResponse withdraw(String uid, BigDecimal amount, String idempotencyKey) {

        User driver = userRepo.findByExternalUidForUpdate(uid)
                .orElseGet(() -> userRepo.findByExternalUid(uid)
                        .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND")));

        final String walletCcy = resolveWalletCurrency(driver);
        amount = scale(walletCcy, amount);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("INVALID_AMOUNT");
        }
        BigDecimal current = Optional.ofNullable(driver.getWalletBalance()).orElse(BigDecimal.ZERO);
        if (current.compareTo(amount) < 0) {
            throw new IllegalArgumentException("INSUFFICIENT_FUNDS");
        }

        // Idempotency (replay-safe)
        final String idemKey = StringUtils.trimToNull(idempotencyKey);
        if (idemKey != null) {
            Optional<WalletTransaction> existing = txnRepo.findByUserAndIdempotencyKey(driver, idemKey);
            if (existing.isPresent()) {
                return new WalletBalanceResponse(
                        Optional.ofNullable(driver.getWalletBalance()).orElse(BigDecimal.ZERO),
                        Optional.ofNullable(driver.getPromoBalance()).orElse(BigDecimal.ZERO),
                        Optional.ofNullable(driver.getCreditBalance()).orElse(BigDecimal.ZERO)
                );
            }
        }

        // Payout provider (void) + réf synthétique
        paymentService.transferToBank(driver, amount, walletCcy);
        String providerRef = "PAYOUT-" + System.currentTimeMillis();

        BigDecimal newBal = scale(walletCcy, current.subtract(amount));
        driver.setWalletBalance(newBal);
        if (StringUtils.isBlank(driver.getDefaultCurrency())) {
            driver.setDefaultCurrency(walletCcy);
        }
        userRepo.save(driver);

        txnRepo.save(WalletTransaction.builder()
                .user(driver)
                .type(WalletTxnType.WITHDRAWAL)
                .amount(amount.negate()) // débit
                .currency(walletCcy)
                .reference(providerRef)
                .idempotencyKey(idemKey)
                .build());

        return new WalletBalanceResponse(newBal,
                Optional.ofNullable(driver.getPromoBalance()).orElse(BigDecimal.ZERO),
                Optional.ofNullable(driver.getCreditBalance()).orElse(BigDecimal.ZERO));
    }
}
