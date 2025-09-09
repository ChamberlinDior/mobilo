/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/model/WalletTransaction.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.model;

import com.mobility.auth.model.enums.WalletTxnType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Mouvement de wallet.
 *
 * Règles :
 *  - amount  : montant signé EN DEVISE DU WALLET (positif = crédit, négatif = débit)
 *  - currency: devise du wallet (USD / EUR / XAF)
 *  - idempotencyKey: permet de rejouer sans redébit (unique par user)
 *  - *Original: trace la valeur telle que débité/payant côté PSP avant conversion
 *  - fxRate  : walletAmount = originalAmount * fxRate
 */
@Entity
@Table(
        name = "wallet_transactions",
        indexes = {
                @Index(name = "idx_txn_user_ts", columnList = "user_id, created_at")
        },
        uniqueConstraints = {
                // Unicité de la clé d’idempotence pour un même utilisateur
                @UniqueConstraint(name = "uk_txn_user_idem", columnNames = {"user_id", "idempotency_key"})
        }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class WalletTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ——— Qui ——— */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /* ——— Quoi ——— */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private WalletTxnType type;

    /** Montant signé EN DEVISE DU WALLET : positif = crédit, négatif = débit */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    /** Devise DU WALLET (stockage interne) */
    @Column(nullable = false, length = 3)
    private String currency;

    /** Référence externe (charge/payout/provider/ride_id…) */
    @Column(length = 64)
    private String reference;

    /** Idempotency-Key reçue (pour rejouer sans redébit) */
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    /* ——— Trace FX si la devise d’origine ≠ devise du wallet ——— */
    /** Montant tel que débité côté provider (avant conversion) */
    @Column(name = "amount_original", precision = 12, scale = 2)
    private BigDecimal amountOriginal;

    /** Devise originale telle qu’envoyée au provider (ex. USD, EUR) */
    @Column(name = "currency_original", length = 3)
    private String currencyOriginal;

    /** Taux de conversion appliqué (wallet = original * fxRate) */
    @Column(name = "fx_rate", precision = 18, scale = 8)
    private BigDecimal fxRate;

    /** Nom du provider FX (ex. exchangerate.host) */
    @Column(name = "fx_provider", length = 32)
    private String fxProvider;

    /* ——— Quand ——— */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void preInsert() {
        createdAt = OffsetDateTime.now();
    }
}
