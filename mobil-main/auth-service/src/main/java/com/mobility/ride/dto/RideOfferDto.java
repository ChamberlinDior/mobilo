// FILE : src/main/java/com/mobility/ride/dto/RideOfferDto.java
package com.mobility.ride.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RideOfferDto {

    private Long   rideId;
    private String productType;      // X, POOL, DELIVERY…

    /* géolocalisation */
    private Double pickupLat;
    private Double pickupLng;
    private Double dropoffLat;
    private Double dropoffLng;

    /* adresses « humaines » */
    private String pickupAddress;
    private String dropoffAddress;

    /* tarification */
    private Double totalFare;
    private String currency;

    /* timestamps */
    private String createdAt;        // ISO-8601
    private String scheduledAt;      // ISO-8601 – nullable

    /* métriques */
    private Double distanceKm;       // facultatif
    private Double weightKg;         // nullable pour non-colis
}
