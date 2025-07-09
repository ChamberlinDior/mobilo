package com.mobility.ride.dto;

/**
 * Infos minimales sur le produit (UberX, Comfort, etc.)
 * envoyées dans RideAcceptedPayload.
 */
public record ProductSnippet(
        String code,      // = ProductType.name()
        String label,     // ex. "Comfort"
        String iconUrl    // URL/asset pour l’icône
) {}
