/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/dto/WalletBalanceResponse.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter @AllArgsConstructor
public class WalletBalanceResponse {
    private BigDecimal walletBalance;
    private BigDecimal promoBalance;
    private BigDecimal creditBalance;
}
