/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/model/enums/WalletTxnType.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.model.enums;

public enum WalletTxnType {
    TOP_UP,            // ↗
    RIDE_PAYMENT,      // ↘ (wallet / CB / PayPal…)
    CASH_PAYMENT,      // ↘ Nouveau – règlement direct au chauffeur
    REFUND,            // ↗
    ADJUSTMENT         // ±
}