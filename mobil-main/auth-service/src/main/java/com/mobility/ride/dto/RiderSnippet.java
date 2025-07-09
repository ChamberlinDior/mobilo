package com.mobility.ride.dto;

/**
 * Version légère du rider transmise dans RideAcceptedPayload.
 * (Même structure que DriverSnippet pour simplifier l’affichage.)
 */
public record RiderSnippet(
        Long    id,
        String  firstName,
        String  lastName,
        byte[]  profilePicture,
        String  profilePictureMimeType
) {}
