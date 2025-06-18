package com.mobility.auth.model;

/**
 * Étape d’on-boarding chauffeur/coursier.
 */
public enum OnboardingStep {
    NONE,        // pas encore démarré
    DOCUMENTS,   // upload pièces / KYC en cours
    TRAINING,    // formation (vidéo, quiz) obligatoire
    COMPLETED    // prêt à prendre des courses
}
