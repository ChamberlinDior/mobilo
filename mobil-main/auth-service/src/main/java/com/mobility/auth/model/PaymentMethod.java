// src/main/java/com/mobility/auth/model/PaymentMethod.java
package com.mobility.auth.model;

import com.mobility.auth.model.converter.PaymentProviderConverter;   // si vous utilisez le converter
import com.mobility.auth.model.enums.PaymentProvider;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;

/**
 * Moyen de paiement enregistré par l’utilisateur :
 * Stripe, Apple-Pay, PayPal, Airtel Money, Espèces, …
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(
        name = "payment_methods",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_payment_method_user_token",
                columnNames = {"user_id", "provider", "token"}
        ),
        indexes = {
                @Index(name = "idx_pm_user",       columnList = "user_id"),
                @Index(name = "idx_pm_is_default", columnList = "user_id,is_default")
        }
)
public class PaymentMethod {

    /* ═════════════ Identité / relation ═════════════ */

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /* ═════════════ Métadonnées prestataire ═════════════ */

    /**  Carte, wallet, cash, etc.  (colonne qui manquait) */
    @Column(length = 32, nullable = false)
    private String type = "card";                    // ex.  card / wallet / cash …

    @Convert(converter = PaymentProviderConverter.class)        // si autoApply = false
    @Column(nullable = false, length = 32)
    private PaymentProvider provider = PaymentProvider.STRIPE;

    /** Jeton opaque émis par le PSP (pm_xxx, EC-TOKEN…) – *nullable* pour CASH. */
    @Column(length = 128)
    private String token;

    /* ═════════════ Détails carte (optionnels) ═════════════ */

    @Size(max = 32)
    private String brand;           // VISA, PAYPAL, AIRTEL…

    @Size(min = 4, max = 4)
    private String last4;

    @Min(1) @Max(12)
    private Integer expMonth;

    @Min(2024)
    private Integer expYear;

    /* ═════════════ Statut applicatif ═════════════ */

    @Column(name = "is_default", nullable = false)
    private boolean defaultMethod = false;

    /* ═════════════ Audit ═════════════ */

    @CreationTimestamp
    @Column(nullable = false, updatable = false,
            columnDefinition = "datetime(6) default current_timestamp(6)")
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(columnDefinition =
            "datetime(6) default current_timestamp(6) on update current_timestamp(6)")
    private OffsetDateTime updatedAt;
}
