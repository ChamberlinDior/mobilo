/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/dto/WalletWithdrawRequest.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Charge utile minimale pour un retrait (driver payout).
 * Exemple JSON : { "amount": 150.00 }
 */
public record WalletWithdrawRequest(
        @NotNull
        @DecimalMin(value = "0.01", inclusive = true) // ≥ 0.01
        BigDecimal amount
) {}
