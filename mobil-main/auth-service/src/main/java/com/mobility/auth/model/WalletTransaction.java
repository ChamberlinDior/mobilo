/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/model/WalletTransaction.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.model;

import com.mobility.auth.model.enums.WalletTxnType;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "wallet_transactions",
        indexes = @Index(name = "idx_txn_user_ts", columnList = "user_id, created_at"))
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class WalletTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ——— Qui ——— */
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /* ——— Quoi ——— */
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16)
    private WalletTxnType type;

    /** Montant signé : positif = crédit, négatif = débit */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    /** Référence externe : charge Stripe, ride_id, etc. */
    @Column(length = 64)
    private String reference;

    /* ——— Quand ——— */
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist void preInsert() { createdAt = OffsetDateTime.now(); }
}
