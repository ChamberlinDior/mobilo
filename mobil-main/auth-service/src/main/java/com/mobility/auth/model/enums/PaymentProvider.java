// src/main/java/com/mobility/auth/model/enums/PaymentProvider.java
package com.mobility.auth.model.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Prestataires de paiement reconnus par Proximo.
 */
public enum PaymentProvider {

    /** Carte ou wallet géré via Stripe (par défaut)  */
    STRIPE,

    /** Apple Pay (payment-sheet ou web-flow) */
    APPLE_PAY,

    /** PayPal / Smart Buttons  */
    PAYPAL,

    /** Espèces (paiement direct au chauffeur) */
    CASH,

    /** Airtel Money – Gabon  */
    AIRTEL_MONEY_GA;

    /* ══════════════════════════════════════════════════════════════
       1)  Méthode utilitaire : conversion sûre depuis n’importe
           quelle chaîne (insensible à la casse, tolère espaces).   */
    private static final Map<String, PaymentProvider> LOOKUP =
            Stream.of(values())
                    .collect(Collectors.toMap(p -> p.name(), p -> p));

    /**
     * Parse un libellé quelconque vers l’enum ; lève une
     * IllegalArgumentException si la valeur n’est pas supportée.
     */
    public static PaymentProvider from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Payment provider is null");
        }
        PaymentProvider p = LOOKUP.get(raw.trim().toUpperCase());
        if (p == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + raw);
        }
        return p;
    }

    /* ══════════════════════════════════════════════════════════════
       2)  Helper : équivalent « toUpperCase() » attendu par certains
           anciens mappers / DTO.  On retourne simplement le code
           constant (déjà en MAJUSCULES).                           */
    public String toUpperCase() {
        return name();        // garde toute compatibilité existante
    }

    /* ══════════════════════════════════════════════════════════════
       3)  Alias d’affichage (facultatif) – utile côté front-office. */
    public String displayLabel() {
        return switch (this) {
            case STRIPE          -> "Carte bancaire";
            case APPLE_PAY       -> "Apple Pay";
            case PAYPAL          -> "PayPal";
            case CASH            -> "Espèces";
            case AIRTEL_MONEY_GA -> "Airtel Money";
        };
    }
}
