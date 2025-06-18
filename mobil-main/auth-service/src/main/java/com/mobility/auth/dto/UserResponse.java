package com.mobility.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mobility.auth.model.*;
import lombok.Builder;
import lombok.Getter;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Projection publique complète d’un utilisateur.
 * <p>✔️ Couvre toutes les données utiles côté apps Uber/Lyft : identité,
 * abonnement, portefeuille, stats chauffeur, préférences, KYC, documents…
 * 🚫 Aucune information sensible (hash mot de passe, tokens internes…)</p>
 */
@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponse {

    /* ═══════════ Identité ═══════════ */
    private final Long   id;
    private final String uid;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phoneNumber;
    private final Role   primaryRole;

    /* ═══════════ Profil & rating ═══════════ */
    private final Double  rating;
    private final Integer ratingCount;
    private final String  profilePictureUrl;

    /* ═══════════ Abonnement & fidélité ═══════════ */
    private final SubscriptionTier subscriptionTier;
    private final Integer          loyaltyPoints;

    /* ═══════════ Wallet & crédits ═══════════ */
    private final BigDecimal walletBalance;
    private final BigDecimal promoBalance;
    private final BigDecimal creditBalance;
    private final Boolean    walletLocked;
    private final String     defaultCurrency;

    /* ═══════════ Statistiques chauffeur / coursier ═══════════ */
    private final Long   lifetimeTrips;
    private final Double acceptanceRate;
    private final Double cancellationRate;
    private final Double onTimePickupRate;
    private final Integer parcelCapacityKg;
    private final Boolean canHandleColdChain;

    /* ═══════════ Préférences & accessibilité ═══════════ */
    private final String            preferredLanguage;
    private final MeasurementSystem measurementSystem;
    private final Boolean           accessibilitySeatNeeded;
    private final Boolean           allowPets;

    /* ═══════════ Communication & sécurité ═══════════ */
    private final Boolean marketingEmailsOptIn;
    private final Boolean smsOptIn;
    private final Boolean twoFactorEnabled;

    /* ═══════════ Profil étendu ═══════════ */
    private final List<PaymentMethodResponse> paymentMethods;
    private final List<AddressResponse>       addresses;
    private final KycStatus                   kycStatus;
    private final List<UrlDocument>           documents;
    private final EmergencyContact            emergencyContact;
    private final List<PushTokenResponse>     pushTokens;

    /* ═══════════ Parrainage & fidélité ═══════════ */
    private final ReferralInfo referralInfo;

    /* ═══════════ Favoris / blocages ═══════════ */
    private final List<Long> favoriteDrivers;
    private final List<Long> blockedUsers;

    /* ═══════════ Localisation temps-réel ═══════════ */
    private final Double lastLat;
    private final Double lastLon;
    private final OffsetDateTime lastLocationAt;

    /* ═══════════ Sous-DTOs ═══════════ */
    public static record PaymentMethodResponse(
            String id,
            String type,
            String last4,
            String expiry
    ) {}

    public static record AddressResponse(
            String label,
            String address,
            Double latitude,
            Double longitude
    ) {}

    public static record UrlDocument(
            String type,
            String url
    ) {}

    public static record EmergencyContact(
            String name,
            String phone
    ) {}

    public static record PushTokenResponse(
            String token,
            String deviceType,
            OffsetDateTime createdAt
    ) {}

    public static record ReferralInfo(
            String referralCode,
            Long referredBy,
            Boolean referralBonusEligible
    ) {}
}
