/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/dto/WalletTransactionResponse.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mobility.auth.model.enums.WalletTxnType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * DTO d’une ligne d’historique wallet.
 * Les champs FX sont optionnels et renvoyés si une conversion a eu lieu.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WalletTransactionResponse {
    private Long id;
    private WalletTxnType type;
    private BigDecimal amount;         // Montant EN DEVISE DU WALLET (signé)
    private String currency;           // Devise du wallet
    private String reference;          // Réf. externe (charge/payout/ride_id…)
    private OffsetDateTime createdAt;

    // Trace FX (optionnelle)
    private BigDecimal amountOriginal; // Montant d’origine débité chez le PSP
    private String currencyOriginal;   // Devise d’origine (USD/EUR/…)
    private BigDecimal fxRate;         // amount = amountOriginal * fxRate
    private String fxProvider;         // ex. "exchangerate.host"
}
