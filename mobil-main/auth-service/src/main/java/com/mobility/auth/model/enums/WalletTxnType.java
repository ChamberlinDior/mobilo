/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/model/enums/WalletTxnType.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.model.enums;

/**
 * Types de mouvements dans la table <code>wallet_transactions</code>.
 * Le signe du montant est toujours :
 *   • positif  →  crédit  (argent qui entre)
 *   • négatif  →  débit   (argent qui sort)
 */
public enum WalletTxnType {

    /* ——↑ crédits—— */
    TOP_UP,            // recharge par carte / mobile money
    REFUND,            // remboursement, goodwill, geste co

    /* ——↓ débits —— */
    RIDE_PAYMENT,      // paiement automatique d’une course
    CASH_PAYMENT,      // paiement direct chauffeur — historique uniquement
    WITHDRAWAL,        // retrait du solde vers un compte bancaire  ← AJOUTÉ
    ADJUSTMENT         // correction manuelle ±
}
