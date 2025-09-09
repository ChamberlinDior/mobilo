/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/model/enums/WalletTxnType.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.model.enums;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Types de mouvements dans la table <code>wallet_transactions</code>.
 *
 * Convention de signe (toujours la même) :
 *   • Montant > 0  ⇒  crédit  (argent qui ENTRE dans le wallet)
 *   • Montant < 0  ⇒  débit   (argent qui SORT du wallet)
 *
 * Remarque :
 *  - Certains types (ADJUSTMENT, REVERSAL) peuvent être ± ; on s'appuie alors
 *    sur le signe effectif du montant pour l'écriture comptable.
 */
public enum WalletTxnType {

    /* ——↑ crédits (argent qui entre) —— */
    TOP_UP,           // recharge (carte / mobile money)
    REFUND,           // remboursement (annulation, goodwill)
    PROMO_CREDIT,     // crédit promotionnel / bonus / coupon
    P2P_IN,           // réception P2P d’un autre utilisateur

    /* ——↓ débits (argent qui sort) —— */
    RIDE_PAYMENT,     // paiement automatique d’une course
    CASH_PAYMENT,     // paiement direct chauffeur — historique
    WITHDRAWAL,       // retrait du solde (virement bancaire / mobile money)
    P2P_OUT,          // envoi P2P vers un autre utilisateur
    FEE,              // frais (service, plateforme, FX, litige…)
    CHARGEBACK,       // rétrofacturation / chargeback

    /* —— ± selon le contexte —— */
    ADJUSTMENT,       // correction manuelle ± (ops/finance)
    REVERSAL          // annulation/contre-passation d’un mouvement antérieur ±
    ;

    /* ══════════════════════════════════════════════════════════════
       Parse tolérant (insensible à la casse, espaces)
       ══════════════════════════════════════════════════════════════ */
    private static final Map<String, WalletTxnType> LOOKUP =
            Stream.of(values()).collect(Collectors.toMap(Enum::name, e -> e));

    /**
     * Convertit une chaîne quelconque en enum. Lance IllegalArgumentException si inconnu.
     * Exemple: "top_up", " Top Up ", "WITHDRAWAL" → OK
     */
    public static WalletTxnType from(String raw) {
        if (raw == null) {
            throw new IllegalArgumentException("Transaction type is null");
        }
        String key = raw.trim()
                .toUpperCase()
                .replace('-', '_')
                .replace(' ', '_');
        WalletTxnType t = LOOKUP.get(key);
        if (t == null) {
            throw new IllegalArgumentException("Unsupported wallet transaction type: " + raw);
        }
        return t;
    }

    /* ══════════════════════════════════════════════════════════════
       Aide UX : libellé d’affichage
       ══════════════════════════════════════════════════════════════ */
    public String displayLabel() {
        return switch (this) {
            case TOP_UP        -> "Recharge";
            case REFUND        -> "Remboursement";
            case PROMO_CREDIT  -> "Crédit promotionnel";
            case P2P_IN        -> "Réception P2P";
            case RIDE_PAYMENT  -> "Paiement de course";
            case CASH_PAYMENT  -> "Paiement cash";
            case WITHDRAWAL    -> "Retrait";
            case P2P_OUT       -> "Envoi P2P";
            case FEE           -> "Frais";
            case CHARGEBACK    -> "Rétrofacturation";
            case ADJUSTMENT    -> "Ajustement";
            case REVERSAL      -> "Contre-passation";
        };
    }

    /* ══════════════════════════════════════════════════════════════
       Direction par défaut :
       +1 = crédit typique, -1 = débit typique, 0 = selon le montant
       (utile pour analytics/filtrage ; la vérité reste le signe réel)
       ══════════════════════════════════════════════════════════════ */
    public int defaultDirection() {
        return switch (this) {
            case TOP_UP, REFUND, PROMO_CREDIT, P2P_IN -> +1;
            case RIDE_PAYMENT, CASH_PAYMENT, WITHDRAWAL, P2P_OUT, FEE, CHARGEBACK -> -1;
            case ADJUSTMENT, REVERSAL -> 0;
        };
    }

    /** Typiquement crédit ? (les types ± retournent false) */
    public boolean isTypicallyCredit() {
        return defaultDirection() > 0;
    }

    /** Typiquement débit ? (les types ± retournent false) */
    public boolean isTypicallyDebit() {
        return defaultDirection() < 0;
    }
}
