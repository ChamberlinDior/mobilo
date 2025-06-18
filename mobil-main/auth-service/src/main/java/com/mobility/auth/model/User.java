package com.mobility.auth.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

/**
 * Modèle unifié Passenger / Driver / Courier / Admin.
 * <p>
 * • Compatible ride-hailing, livraison de colis, abonnements & loyalty.<br>
 * • Inclut toutes les métadonnées utilisées par Uber, Lyft, etc.<br>
 * • 2025-05 : prise en charge des photos de profil BLOB (LONGBLOB + MIME).<br>
 *   L’ancienne clé objet stockage est conservée pour une migration douce.
 * </p>
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_email", columnNames = "email"),
                @UniqueConstraint(name = "uk_users_phone", columnNames = "phone_number"),
                @UniqueConstraint(name = "uk_users_uid",   columnNames = "external_uid")
        },
        indexes = {
                @Index(name = "idx_users_role",         columnList = "primary_role"),
                @Index(name = "idx_users_kyc_status",   columnList = "kyc_status"),
                @Index(name = "idx_users_rating",       columnList = "rating"),
                @Index(name = "idx_users_subscription", columnList = "subscription_tier")
        }
)
@Getter @Setter @Builder
@NoArgsConstructor @AllArgsConstructor
public class User {

    /* ───────────────────────── Identité & contact ────────────────────────── */
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_uid", nullable = false, length = 36)
    private String externalUid;

    @Email @NotBlank
    @Column(nullable = false, length = 160)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 72)
    private String passwordHash;

    @Pattern(regexp = "^\\+?[1-9]\\d{7,14}$")
    @Column(name = "phone_number", nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 60) private String firstName;
    @Column(nullable = false, length = 60) private String lastName;
    private OffsetDateTime dateOfBirth;

    /* ───────────────────── Photo de profil (BLOB + MIME) ─────────────────── */
    /** Octets de l’image (LONGBLOB ≤ 4 Go). */
    @Lob
    @Column(name = "profile_picture", columnDefinition = "LONGBLOB")
    private byte[] profilePicture;

    /** Type MIME (image/jpeg, image/png, …). */
    @Column(name = "profile_picture_mime", length = 32)
    private String profilePictureMimeType;

    /** Clé objet stockage historique (S3, GCS…). Maintenue pour migration. */
    @Column(length = 255)
    private String profilePictureKey;

    /* ─────────────── Vérifications & conformité ─────────────── */
    private Boolean emailVerified;
    private Boolean phoneVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyc_status", nullable = false, length = 16)
    private KycStatus kycStatus;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<UserDocument> kycDocuments;

    /* ─────────────── Légalité chauffeur & onboarding ─────────── */
    @Column(length = 32) private String driverLicenseNumber;
    private OffsetDateTime driverLicenseExp;

    @Enumerated(EnumType.STRING) private BackgroundCheckStatus backgroundCheckStatus;
    @Enumerated(EnumType.STRING) private InspectionStatus vehicleInspectionStatus;
    @Enumerated(EnumType.STRING) private OnboardingStep onboardingStep;

    /* ─────────────── Rôles, rating, sécurité ────────────────── */
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_role", nullable = false, length = 12)
    private Role primaryRole;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", length = 12)
    @Enumerated(EnumType.STRING)
    private Set<Role> extraRoles;

    private Double  rating;
    private Integer ratingCount;

    @Column(length = 4) private String safetyPin;
    private Boolean safetyAudioOptIn;
    private OffsetDateTime lastSafetyIncidentAt;
    private String deactivationReason;

    /* ─────────────── Wallet, paiements & crédits ─────────────── */
    @Column(precision = 12, scale = 2) private BigDecimal walletBalance;
    @Column(precision = 12, scale = 2) private BigDecimal promoBalance;
    @Column(precision = 12, scale = 2) private BigDecimal creditBalance;
    private Boolean walletLocked;

    @Column(length = 64) private String defaultPaymentProfileId;
    @Column(length = 64) private String stripeAccountId;
    private Boolean payoutOnHold;

    @Enumerated(EnumType.STRING) private TaxFormStatus taxFormStatus;
    private Boolean twoFactorEnabled;

    /* ─────────────── Profil complet : préférences & notif ────── */
    @Enumerated(EnumType.STRING) private MeasurementSystem measurementSystem;
    private Boolean marketingEmailsOptIn;
    private Boolean smsOptIn;

    /* Localisation & devise */
    private String timezone;
    private String defaultCurrency;

    /* Contact d’urgence */
    private String emergencyContactName;
    private String emergencyContactPhone;

    /* Moyens de paiement externes */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentMethod> paymentMethods;

    /* Adresses */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Address> addresses;

    /* Push-tokens */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL,
            orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PushToken> pushTokens;

    /* ─────────────── Parrainage & fidélité ───────────────────── */
    @Column(length = 10) private String referralCode;
    private Long referredBy;
    private Boolean referralBonusEligible;

    /* ─────────────── Favoris / blocages ──────────────────────── */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_favorites", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "favorite_driver_id")
    private Set<Long> favoriteDrivers;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "user_blocked", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "blocked_user_id")
    private Set<Long> blockedUsers;

    /* ─────────────── Abonnements & fidélité ──────────────────── */
    @Enumerated(EnumType.STRING) private SubscriptionTier subscriptionTier;
    private OffsetDateTime subscriptionRenewAt;
    private Integer loyaltyPoints;

    /* ─────────────── Statistiques chauffeur ──────────────────── */
    private Long   lifetimeTrips;
    private Double acceptanceRate;
    private Double cancellationRate;
    private Double onTimePickupRate;
    private Integer parcelCapacityKg;
    private Boolean canHandleColdChain;

    /* ─────────────── Préférences client ──────────────────────── */
    private String  preferredLanguage;
    private Boolean accessibilitySeatNeeded;
    private Boolean allowPets;
    private Boolean allowSmoking;

    /* ─────────────── Localisation temps-réel ─────────────────── */
    private Double lastLat;
    private Double lastLon;
    private OffsetDateTime lastLocationAt;

    /* ─────────────── Acceptations légales ────────────────────── */
    private OffsetDateTime tosAcceptedAt;
    private OffsetDateTime privacyAcceptedAt;
    private Boolean ageVerified;

    /* ─────────────── États & métadonnées ─────────────────────── */
    @Enumerated(EnumType.STRING)
    @Column(length = 12)
    private AccountStatus accountStatus;

    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime archivedAt;

    /* ─────────────── Hooks JPA ───────────────────────────────── */
    @PrePersist
    protected void onCreate() {
        this.createdAt     = OffsetDateTime.now();
        this.accountStatus = AccountStatus.ACTIVE;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
