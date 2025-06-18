// src/main/java/com/mobility/auth/mapper/UserMapper.java
package com.mobility.auth.mapper;

import com.mobility.auth.dto.SignUpRequest;
import com.mobility.auth.dto.UserResponse;
import com.mobility.auth.model.Role;
import com.mobility.auth.model.User;
import org.mapstruct.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * MapStruct – conversion <strong>User ⇆ DTO</strong>.
 */
@Mapper(
        componentModel       = "spring",
        injectionStrategy    = InjectionStrategy.CONSTRUCTOR,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        /* ───── NOUVEAU : on déclare le mapper à réutiliser ───── */
        uses                 = PaymentMethodMapper.class,
        imports              = {
                UUID.class,
                BigDecimal.class,
                OffsetDateTime.class,
                UserResponse.EmergencyContact.class,
                UserResponse.ReferralInfo.class
        }
)
public interface UserMapper {

    /* ═════════════ SignUpRequest ➜ User ═════════════ */

    @BeanMapping(builder = @Builder(disableBuilder = true))
    @Mapping(target = "id",           ignore = true)
    @Mapping(target = "externalUid",  expression = "java(UUID.randomUUID().toString())")
    @Mapping(target = "email",        source = "req.email")
    @Mapping(target = "passwordHash", expression = "java(passwordHash)")
    @Mapping(target = "phoneNumber",  source = "req.phoneNumber")
    @Mapping(target = "firstName",    source = "req.firstName")
    @Mapping(target = "lastName",     source = "req.lastName")
    @Mapping(target = "primaryRole",  expression = "java(role)")
    @Mapping(target = "referralCode", source = "req.referralCode")

    /* valeurs par défaut */
    @Mapping(target = "emailVerified",    constant = "false")
    @Mapping(target = "phoneVerified",    constant = "false")
    @Mapping(target = "kycStatus",        constant = "NONE")
    @Mapping(target = "walletBalance",    expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "promoBalance",     expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "creditBalance",    expression = "java(BigDecimal.ZERO)")
    @Mapping(target = "twoFactorEnabled", constant = "false")
    @Mapping(target = "subscriptionTier", constant = "NONE")
    @Mapping(target = "loyaltyPoints",    constant = "0")
    @Mapping(target = "createdAt",        expression = "java(OffsetDateTime.now())")
    @Mapping(target = "accountStatus",    constant = "ACTIVE")

    /* non concernés à l’inscription */
    @Mapping(target = "profilePicture",         ignore = true)
    @Mapping(target = "profilePictureMimeType", ignore = true)
    @Mapping(target = "profilePictureKey",      ignore = true)
    User toEntity(
            SignUpRequest req,
            @Context Role role,
            @Context String passwordHash
    );

    /* ═════════════ User ➜ UserResponse ═════════════ */

    @Mapping(target = "uid",           source = "externalUid")
    @Mapping(target = "email",         source = "email")
    @Mapping(target = "firstName",     source = "firstName")
    @Mapping(target = "lastName",      source = "lastName")
    @Mapping(target = "phoneNumber",   source = "phoneNumber")
    @Mapping(target = "primaryRole",   source = "primaryRole")
    @Mapping(target = "rating",        source = "rating")
    @Mapping(target = "ratingCount",   source = "ratingCount")

    /* photo : expose l’endpoint si le BLOB existe */
    @Mapping(
            target      = "profilePictureUrl",
            expression  = "java(user.getProfilePicture()!=null ? \"/api/v1/users/me/picture\" : null)"
    )

    /* Wallet & monnaie */
    @Mapping(target = "walletBalance",      source = "walletBalance")
    @Mapping(target = "promoBalance",       source = "promoBalance")
    @Mapping(target = "creditBalance",      source = "creditBalance")
    @Mapping(target = "walletLocked",       source = "walletLocked")
    @Mapping(target = "defaultCurrency",    source = "defaultCurrency")

    /* Stats chauffeur */
    @Mapping(target = "lifetimeTrips",      source = "lifetimeTrips")
    @Mapping(target = "acceptanceRate",     source = "acceptanceRate")
    @Mapping(target = "cancellationRate",   source = "cancellationRate")
    @Mapping(target = "onTimePickupRate",   source = "onTimePickupRate")
    @Mapping(target = "parcelCapacityKg",   source = "parcelCapacityKg")
    @Mapping(target = "canHandleColdChain", source = "canHandleColdChain")

    /* Préférences & accessibilité */
    @Mapping(target = "preferredLanguage",       source = "preferredLanguage")
    @Mapping(target = "measurementSystem",       source = "measurementSystem")
    @Mapping(target = "accessibilitySeatNeeded", source = "accessibilitySeatNeeded")
    @Mapping(target = "allowPets",               source = "allowPets")
    @Mapping(target = "marketingEmailsOptIn",    source = "marketingEmailsOptIn")
    @Mapping(target = "smsOptIn",                source = "smsOptIn")
    @Mapping(target = "twoFactorEnabled",        source = "twoFactorEnabled")

    /* Collections (⚠︎ délégué à PaymentMethodMapper) */
    @Mapping(target = "paymentMethods", source = "paymentMethods")
    @Mapping(target = "addresses",      source = "addresses")
    @Mapping(target = "kycStatus",      source = "kycStatus")
    @Mapping(target = "documents",      ignore = true) // géré ailleurs
    @Mapping(target = "pushTokens",     source = "pushTokens")

    /* Contact d’urgence */
    @Mapping(
            target = "emergencyContact",
            expression = "java(user.getEmergencyContactName()!=null ? " +
                    "new UserResponse.EmergencyContact(" +
                    "user.getEmergencyContactName(), user.getEmergencyContactPhone()) : null)"
    )

    /* Parrainage */
    @Mapping(
            target = "referralInfo",
            expression = "java(new UserResponse.ReferralInfo(" +
                    "user.getReferralCode(), user.getReferredBy(), user.getReferralBonusEligible()))"
    )

    /* Favoris / blocages */
    @Mapping(target = "favoriteDrivers", source = "favoriteDrivers")
    @Mapping(target = "blockedUsers",    source = "blockedUsers")

    /* Abonnement & fidélité */
    @Mapping(target = "subscriptionTier", source = "subscriptionTier")
    @Mapping(target = "loyaltyPoints",    source = "loyaltyPoints")

    /* Position */
    @Mapping(target = "lastLat",        source = "lastLat")
    @Mapping(target = "lastLon",        source = "lastLon")
    @Mapping(target = "lastLocationAt", source = "lastLocationAt")
    UserResponse toResponse(User user);
}
