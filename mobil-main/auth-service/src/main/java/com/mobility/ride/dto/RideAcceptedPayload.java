// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/dto/RideAcceptedPayload.java
//  v2025-09-04 – tout-en-un pour l’écran « match »
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.dto;

import java.time.OffsetDateTime;

/**
 * Transmis dès qu’un chauffeur accepte la course.
 * Contient tout ce que rider & driver doivent afficher :
 * avatars, produit, et horodatage.
 */
public record RideAcceptedPayload(
        Long            rideId,
        DriverSnippet   driver,
        RiderSnippet    rider,
        ProductSnippet  product,
        OffsetDateTime  acceptedAt
) {}
