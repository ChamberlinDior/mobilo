package com.mobility.auth.model;

/**
 * Avancement des formulaires fiscaux (IRS W-9, 1099, etc.).
 */
public enum TaxFormStatus {
    NONE,        // non démarré
    PENDING,     // requis mais non soumis
    SUBMITTED,   // envoyé, en attente d’approbation
    ACCEPTED     // validé par le service finance
}
