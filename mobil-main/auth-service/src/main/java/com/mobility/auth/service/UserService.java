// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/auth/service/UserService.java
//  v2025-09-13 – + getProfilePictureById(Long userId)
// ─────────────────────────────────────────────────────────────
package com.mobility.auth.service;

import com.mobility.auth.dto.*;
import com.mobility.auth.mapper.UserMapper;
import com.mobility.auth.model.*;
import com.mobility.auth.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service principal de gestion des utilisateurs.
 *
 * • Authentification, profil, KYC, favoris…
 * • 2025-05 : photo de profil stockée en BLOB dans la table <em>users</em>.
 * • 2025-07 : champs chauffeur (permis, inspection, etc.) mis à jour via PATCH / users/me.
 * • 2025-09-13 : lecture de photo par ID numérique (getProfilePictureById).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    /* ───────────────────── Dépôts & utilitaires ───────────────────── */
    private final UserRepository          userRepo;
    private final PaymentMethodRepository paymentMethodRepo;
    private final AddressRepository       addressRepo;
    private final PushTokenRepository     pushTokenRepo;
    private final UserDocumentRepository  documentRepo;
    private final RefreshTokenRepository  refreshTokenRepo;
    private final StorageService          storageService;
    private final UserMapper              mapper;
    private final PasswordEncoder         passwordEncoder;
    private final AuthenticationManager   authManager;
    private final JwtService              jwtService;
    private final EmailVerificationService emailVerificationService;

    private static final Duration ACCESS_TTL  = Duration.ofMinutes(15);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    /* ═════════════════════ AUTHENTIFICATION ═════════════════════ */

    @Transactional
    public TokenResponse signUp(SignUpRequest req, Role role) {
        if (userRepo.existsByEmail(req.email()))             throw new IllegalArgumentException("EMAIL_TAKEN");
        if (userRepo.existsByPhoneNumber(req.phoneNumber())) throw new IllegalArgumentException("PHONE_TAKEN");

        String hash = passwordEncoder.encode(req.password());
        User user   = mapper.toEntity(req, role, hash);
        user.setWalletBalance(BigDecimal.ZERO);
        user.setPromoBalance(BigDecimal.ZERO);
        user.setCreditBalance(BigDecimal.ZERO);
        user.setWalletLocked(false);
        user.setEmailVerified(false);
        userRepo.save(user);

        emailVerificationService.createAndSendToken(user);
        return issueTokens(user, null);
    }

    @Transactional
    public TokenResponse authenticate(LoginRequest req) {
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.email(), req.password())
        );
        User user = userRepo.findByEmail(req.email())
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));
        return issueTokens(user, req.deviceId());
    }

    @Transactional
    public TokenResponse refresh(String refreshToken, String deviceId) {
        RefreshToken rt = refreshTokenRepo.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("INVALID_TOKEN"));
        if (rt.isExpired() || Boolean.TRUE.equals(rt.getRevoked()))
            throw new IllegalArgumentException("EXPIRED_TOKEN");

        rt.setRevoked(true);
        refreshTokenRepo.save(rt);
        return issueTokens(rt.getUser(), deviceId);
    }

    @Transactional
    public void changePassword(String uid, String oldPass, String newPass) {
        User user = findUser(uid);
        if (!passwordEncoder.matches(oldPass, user.getPasswordHash()))
            throw new IllegalArgumentException("BAD_CREDENTIALS");

        user.setPasswordHash(passwordEncoder.encode(newPass));
        userRepo.save(user);
        refreshTokenRepo.revokeAllForUser(user.getId());
    }

    /* ─────────── Génération JWT + RefreshToken ─────────── */
    private TokenResponse issueTokens(User user, String deviceId) {
        OffsetDateTime now = OffsetDateTime.now();

        String access = jwtService.generateToken(user, now.plus(ACCESS_TTL));
        String refreshVal = UUID.randomUUID().toString();

        RefreshToken rt = RefreshToken.builder()
                .token(refreshVal)
                .user(user)
                .expiresAt(now.plus(REFRESH_TTL))
                .revoked(false)
                .build();
        refreshTokenRepo.save(rt);

        return TokenResponse.builder()
                .tokenType("Bearer")
                .accessToken(access)
                .expiresIn(ACCESS_TTL.toSeconds())
                .refreshToken(refreshVal)
                .refreshExpiresIn(REFRESH_TTL.toSeconds())
                .deviceId(deviceId)
                .issuedAt(now)
                .user(mapper.toResponse(user))
                .build();
    }

    /* ═════════════════════ PROFIL UTILISATEUR ═════════════════════ */

    @Transactional(readOnly = true)
    public UserResponse getPublicProfile(String uid) {
        return mapper.toResponse(findUser(uid));
    }

    /**
     * Applique le patch partiel reçu de l’API mobile.
     * Seuls les champs non nuls sont appliqués.
     */
    @Transactional
    public UserResponse updateProfile(String uid, UpdateUserRequest req) {
        User u = findUser(uid);

        /* ─────────── Identité & préférences ─────────── */
        if (req.firstName()               != null)  u.setFirstName(req.firstName());
        if (req.lastName()                != null)  u.setLastName(req.lastName());
        if (req.preferredLanguage()       != null)  u.setPreferredLanguage(req.preferredLanguage());
        if (req.measurementSystem()       != null)  u.setMeasurementSystem(req.measurementSystem());
        if (req.marketingEmailsOptIn()    != null)  u.setMarketingEmailsOptIn(req.marketingEmailsOptIn());
        if (req.smsOptIn()                != null)  u.setSmsOptIn(req.smsOptIn());
        if (req.timezone()                != null)  u.setTimezone(req.timezone());
        if (req.defaultCurrency()         != null)  u.setDefaultCurrency(req.defaultCurrency());

        /* ─────────── Champs chauffeur (07-2025) ─────────── */
        if (req.driverLicenseNumber()     != null)  u.setDriverLicenseNumber(req.driverLicenseNumber());
        if (req.driverLicenseExp()        != null)  u.setDriverLicenseExp(
                OffsetDateTime.parse(req.driverLicenseExp() + "T00:00:00Z"));
        if (req.parcelCapacityKg()        != null)  u.setParcelCapacityKg(req.parcelCapacityKg());
        if (req.canHandleColdChain()      != null)  u.setCanHandleColdChain(req.canHandleColdChain());
        if (req.vehicleInspectionStatus() != null)  u.setVehicleInspectionStatus(req.vehicleInspectionStatus());
        if (req.backgroundCheckStatus()   != null)  u.setBackgroundCheckStatus(req.backgroundCheckStatus());

        /* ─────────── Contact d’urgence ─────────── */
        if (req.emergencyContact() != null) {
            u.setEmergencyContactName (req.emergencyContact().name());
            u.setEmergencyContactPhone(req.emergencyContact().phone());
        }

        userRepo.save(u);
        return mapper.toResponse(u);
    }

    /* ═════════════════════ COMPOSANTS ANNEXES ═════════════════════ */
    /* … (paiements, adresses, push-tokens, favoris, documents : inchangés) */

    /* Paiements */
    @Transactional(readOnly = true)
    public List<PaymentMethod> getPaymentMethods(String uid) {
        return paymentMethodRepo.findAllByUser(findUser(uid));
    }
    @Transactional
    public PaymentMethod addPaymentMethod(String uid, PaymentMethod m) {
        m.setUser(findUser(uid));
        return paymentMethodRepo.save(m);
    }
    @Transactional
    public void deletePaymentMethod(String uid, Long id) {
        paymentMethodRepo.deleteByIdAndUser(id, findUser(uid));
    }

    /* Adresses */
    @Transactional(readOnly = true)
    public List<Address> getAddresses(String uid) {
        return addressRepo.findAllByUser(findUser(uid));
    }
    @Transactional
    public Address addAddress(String uid, Address a) {
        a.setUser(findUser(uid));
        return addressRepo.save(a);
    }
    @Transactional
    public void deleteAddress(String uid, Long id) {
        addressRepo.deleteByIdAndUser(id, findUser(uid));
    }

    /* Contact d'urgence */
    @Transactional(readOnly = true)
    public EmergencyContact getEmergencyContact(String uid) {
        User u = findUser(uid);
        return EmergencyContact.builder()
                .name(u.getEmergencyContactName())
                .phone(u.getEmergencyContactPhone())
                .build();
    }
    @Transactional
    public EmergencyContact updateEmergencyContact(String uid, EmergencyContact c) {
        User u = findUser(uid);
        u.setEmergencyContactName(c.getName());
        u.setEmergencyContactPhone(c.getPhone());
        userRepo.save(u);
        return c;
    }

    /* 2-FA */
    @Transactional
    public boolean toggleTwoFactor(String uid, boolean enabled) {
        User u = findUser(uid);
        u.setTwoFactorEnabled(enabled);
        userRepo.save(u);
        return enabled;
    }

    /* Push-tokens */
    @Transactional(readOnly = true)
    public List<PushToken> listPushTokens(String uid) {
        return pushTokenRepo.findAllByUser(findUser(uid));
    }
    @Transactional
    public PushToken addPushToken(String uid, PushToken t) {
        t.setUser(findUser(uid));
        return pushTokenRepo.save(t);
    }
    @Transactional
    public void deletePushToken(String uid, String token) {
        pushTokenRepo.deleteByTokenAndUser(token, findUser(uid));
    }

    /* KYC & documents */
    @Transactional(readOnly = true)
    public KycStatus getKycStatus(String uid) {
        return findUser(uid).getKycStatus();
    }
    @Transactional
    public UrlDocument uploadDocument(String uid, MultipartFile file, String docType) throws Exception {
        User u = findUser(uid);
        String key = storageService.storeDocument(file.getBytes(), file.getContentType());

        UserDocument d = UserDocument.builder()
                .user(u)
                .documentType(docType)
                .filename(file.getOriginalFilename())
                .mimeType(file.getContentType())
                .dataKey(key)
                .build();
        documentRepo.save(d);

        return UrlDocument.builder()
                .type(docType)
                .url(storageService.getPresignedUrl(key))
                .build();
    }
    @Transactional(readOnly = true)
    public KycDocumentPage listDocuments(String uid) {
        List<UrlDocument> docs = documentRepo.findAllByUser(findUser(uid))
                .stream()
                .map(d -> UrlDocument.builder()
                        .type(d.getDocumentType())
                        .url(storageService.getPresignedUrl(d.getDataKey()))
                        .build())
                .collect(Collectors.toList());
        return KycDocumentPage.builder().documents(docs).build();
    }

    /* Favoris / blocages */
    @Transactional
    public List<Long> addFavoriteDriver(String uid, Long driverId) {
        User u = findUser(uid);
        u.getFavoriteDrivers().add(driverId);
        userRepo.save(u);
        return List.copyOf(u.getFavoriteDrivers());
    }
    @Transactional
    public List<Long> removeFavoriteDriver(String uid, Long driverId) {
        User u = findUser(uid);
        u.getFavoriteDrivers().remove(driverId);
        userRepo.save(u);
        return List.copyOf(u.getFavoriteDrivers());
    }
    @Transactional
    public List<Long> blockUser(String uid, Long toBlock) {
        User u = findUser(uid);
        u.getBlockedUsers().add(toBlock);
        userRepo.save(u);
        return List.copyOf(u.getBlockedUsers());
    }
    @Transactional
    public List<Long> unblockUser(String uid, Long toUnblock) {
        User u = findUser(uid);
        u.getBlockedUsers().remove(toUnblock);
        userRepo.save(u);
        return List.copyOf(u.getBlockedUsers());
    }

    /* ═══════════════ PHOTO DE PROFIL (BLOB) ═══════════════ */

    @Transactional
    public void saveProfilePicture(String uid, String mimeType, byte[] data) {
        if (data == null || data.length == 0) throw new IllegalArgumentException("EMPTY_IMAGE");
        User u = findUser(uid);
        u.setProfilePicture(data);
        u.setProfilePictureMimeType(mimeType);
        u.setProfilePictureKey(null);
        userRepo.save(u);
    }

    @Transactional(readOnly = true)
    public ProfilePicture getProfilePicture(String uid) {
        User u = findUser(uid);
        byte[] img = u.getProfilePicture();
        return (img == null) ? null : new ProfilePicture(img, u.getProfilePictureMimeType());
    }

    /** ▼ NOUVEAU : récupération de la photo par ID interne (Long) */
    @Transactional(readOnly = true)
    public ProfilePicture getProfilePictureById(Long userId) {
        User u = userRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));
        byte[] img = u.getProfilePicture();
        return (img == null) ? null : new ProfilePicture(img, u.getProfilePictureMimeType());
    }

    /* ───────────────────── Utilitaire interne ───────────────────── */
    private User findUser(String uid) {
        return userRepo.findByExternalUid(uid)
                .orElseThrow(() -> new EntityNotFoundException("USER_NOT_FOUND"));
    }
}
