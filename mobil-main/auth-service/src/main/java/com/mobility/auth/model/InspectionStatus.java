package com.mobility.auth.model;

/**
 * Statut d’inspection du véhicule chauffeur / coursier.
 */
public enum InspectionStatus {
    VALID,      // inspection valide
    EXPIRED,    // date de validité dépassée
    PENDING     // inspection planifiée ou en attente de validation
}
