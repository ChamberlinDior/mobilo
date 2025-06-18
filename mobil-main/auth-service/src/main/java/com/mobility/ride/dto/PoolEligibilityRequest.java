/* ------------------------------------------------------------------ */
/* PoolEligibilityRequest.java                                        */
/* ------------------------------------------------------------------ */
package com.mobility.ride.dto;

import com.mobility.ride.model.LatLng;
import com.mobility.ride.model.ProductType;
import jakarta.validation.constraints.*;

public record PoolEligibilityRequest(
        @NotNull Long        userId,
        @NotNull ProductType productType,
        @NotNull LatLng      pickup,
        @NotNull LatLng      dropoff,
        @Min(1) @Max(2) int  seatsRequested
) {}
