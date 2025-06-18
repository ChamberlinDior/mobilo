/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/dto/WalletTransactionResponse.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.dto;

import com.mobility.auth.model.enums.WalletTxnType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter @Builder
public class WalletTransactionResponse {
    private Long id;
    private WalletTxnType type;
    private BigDecimal amount;
    private String currency;
    private String reference;
    private OffsetDateTime createdAt;
}
