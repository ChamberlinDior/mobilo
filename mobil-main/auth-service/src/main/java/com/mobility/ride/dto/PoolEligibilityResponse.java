/* ------------------------------------------------------------------
 * PoolEligibilityResponse.java
 * ------------------------------------------------------------------ */
package com.mobility.ride.dto;

import lombok.*;

/** Résultat du check d’éligibilité « Pool ». */
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class PoolEligibilityResponse {
    private Boolean eligible;
    private String  reason;   // null si eligible=true
}