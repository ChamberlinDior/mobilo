package com.mobility.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mobility.auth.model.MeasurementSystem;
import com.mobility.auth.model.BackgroundCheckStatus;
import com.mobility.auth.model.InspectionStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.util.List;

/**
 * Patch partiel du profil utilisateur.
 * <p>Seuls les champs non-nuls seront appliqués.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record UpdateUserRequest(

        /* ═ Identité ═ */
        @Size(max = 60) String firstName,
        @Size(max = 60) String lastName,

        /* ═ Préférences ═ */
        @Size(max = 8)  String preferredLanguage,      // fr, en…
        MeasurementSystem measurementSystem,          // METRIC / IMPERIAL

        /* ═ Marketing & notifications ═ */
        Boolean marketingEmailsOptIn,
        Boolean smsOptIn,

        /* ═ Profil étendu ═ */
        @Size(max = 64) String timezone,
        @Size(max = 3)  String defaultCurrency,

        /* ═ Chauffeur & conformité ═ */
        @Size(max = 32)                      String  driverLicenseNumber,
        /** ISO-8601 « YYYY-MM-DD »  */
        @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$")   String  driverLicenseExp,
        @Min(0) @Max(500)                             Integer parcelCapacityKg,
        Boolean                                       canHandleColdChain,
        InspectionStatus                              vehicleInspectionStatus,
        BackgroundCheckStatus                         backgroundCheckStatus,

        /* ═ Contact d'urgence ═ */
        EmergencyContact emergencyContact,

        /* ═ Collections embarquées ═ */
        List<PaymentMethodRequest> paymentMethods,
        List<AddressRequest>       addresses
) {
    /* ────────────────── Sous-DTOs ────────────────── */

    /** DTO interne pour le contact d'urgence */
    public static record EmergencyContact(
            @NotBlank @Size(max = 60) String name,
            @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$")
            String phone
    ) {}

    /** DTO interne pour un moyen de paiement */
    public static record PaymentMethodRequest(
            @NotBlank String type,
            @NotBlank String token
    ) {}

    /** DTO interne pour une adresse utilisateur */
    public static record AddressRequest(
            @NotBlank String label,
            @NotBlank String address,
            Double latitude,
            Double longitude
    ) {}
}
