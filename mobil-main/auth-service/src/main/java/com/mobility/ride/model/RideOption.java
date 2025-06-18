// src/main/java/com/mobility/ride/model/RideOption.java
package com.mobility.ride.model;

/**
 * Flags optionnels pour une course.
 * Permettent de préciser des préférences (sécurité, confort, culture).
 */
public enum RideOption {
    PETS,            // Animaux autorisés
    BABY_SEAT,       // Siège bébé
    BOOSTER_SEAT,    // Rehausseur
    ACCESSIBILITY,   // Accessible fauteuil roulant
    LUGGAGE,         // Espace bagages
    BIKE_RACK,       // Porte-vélo

    SILENT,          // Mode silencieux
    TEMPERATURE,     // Contrôle température
    MUSIC,           // Musique à bord
    PHONE_CHARGER,   // Chargeur téléphone
    WIFI,            // Wi-Fi embarqué

    FEMALE_DRIVER,   // Chauffeur femme uniquement
    CASH_PREFERRED,  // Préfère espèces
    LANGUAGE_FR,     // Préfère français
    LANGUAGE_EN,     // Préfère anglais

    FACE_MASK,       // Chauffeur masqué
    DISINFECTED_SEAT,// Siège désinfecté

    AUDIO_RECORDING, // Audio recording opt-in
    LIGHT_PANEL      // Tableau lumineux nuit
}
