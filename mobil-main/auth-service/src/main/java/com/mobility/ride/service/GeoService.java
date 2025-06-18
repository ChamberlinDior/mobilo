package com.mobility.ride.service;

public interface GeoService {

    /**
     * Géocodage inverse : (lat,lng) ➜ adresse lisible.
     * Jamais {@code null} (chaine de secours sinon).
     */
    String reverse(Double lat, Double lng);
}
