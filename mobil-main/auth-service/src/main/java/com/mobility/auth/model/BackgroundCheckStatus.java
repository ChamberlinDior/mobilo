package com.mobility.auth.model;

/**
 * Résultat du contrôle d’antécédents (Checkr / Onfido).
 */
public enum BackgroundCheckStatus {
    CLEAR,     // aucun incident
    REVIEW,    // en cours d’examen manuel
    SUSPENDED  // bloqué pour motif sécurité
}
