// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/dto/DriverSnippet.java
//  v2025-09-04 – DTO “avatar” chauffeur (6 champs)
// ─────────────────────────────────────────────────────────────
package com.mobility.ride.dto;

/**
 * Informations minimales affichées dans la carte chauffeur
 * après acceptation d’une course.
 */
public record DriverSnippet(
        Long   id,
        String firstName,
        String lastName,
        byte[] profilePicture,
        String profilePictureMimeType,
        Double rating
) {}
