/* --------------------------------------------------------------------
 * src/main/java/com/mobility/auth/dto/WalletTopUpRequest.java
 * ------------------------------------------------------------------*/
package com.mobility.auth.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class WalletTopUpRequest {

    @NotNull @Positive
    private BigDecimal amount;

    @NotBlank @Size(min = 3, max = 3)
    private String currency;

    /** paymentMethodId retourné par /payment-methods */
    @NotBlank
    private String paymentMethodId;
}
