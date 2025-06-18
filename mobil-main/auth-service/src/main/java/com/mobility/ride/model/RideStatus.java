// src/main/java/com/mobility/ride/model/RideStatus.java
package com.mobility.ride.model;

/**
 * Cycle de vie d’une course.
 */
public enum RideStatus {
    REQUESTED,   // Demande reçue
    ACCEPTED,    // Chauffeur assigné
    SCHEDULED,   // Planifiée (réservation future)
    IN_PROGRESS, // En cours (prise en charge effectuée)
    COMPLETED,   // Terminée
    CANCELLED    // Annulée
}
