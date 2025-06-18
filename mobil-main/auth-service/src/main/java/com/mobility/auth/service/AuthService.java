package com.mobility.auth.service;

import com.mobility.auth.dto.*;
import com.mobility.auth.model.Role;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <h2>Auth Service – façade d’authentification</h2>
 *
 * <p>
 *   Couche mince exposée aux controllers REST / gRPC : délègue la logique
 *   de création d’utilisateur et de gestion des jetons à {@link UserService},
 *   applique les règles métier haut-niveau (choix des rôles, vérification
 *   pré-inscription, etc.) et centralise le logging sécurité.<br>
 *   <br>
 *   ➤ Avantages :<br>
 *   • Maintient les controllers « clean » (thin controller / rich service).<br>
 *   • Point d’extension unique pour ajouter MFA, reCAPTCHA, rate-limiting IP,
 *     etc. sans toucher à la persistance ou au mapping.<br>
 *   • Transactions propagées : toute la logique du {@link UserService} reste
 *     alignée (ACID) tout en exposant une API simple aux contrôleurs.
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;

    /* ═══════════ Sign-up (Rider) ═══════════ */
    @Transactional
    public TokenResponse signUpRider(@Valid SignUpRequest req) {
        log.info("Sign-up rider email={} phone={}", req.email(), req.phoneNumber());
        return userService.signUp(req, Role.RIDER);
    }

    /* ═══════════ Sign-up (Driver | Courier) ═══════════ */
    @Transactional
    public TokenResponse signUpDriver(@Valid SignUpRequest req) {
        log.info("Sign-up driver email={} phone={}", req.email(), req.phoneNumber());
        return userService.signUp(req, Role.DRIVER);
    }

    /* ═══════════ Login (any role) ═══════════ */
    @Transactional
    public TokenResponse login(@Valid LoginRequest req) {
        log.debug("Login attempt email={} device={}", req.email(), req.deviceId());
        return userService.authenticate(req);
    }

    /* ═══════════ Refresh Token ═══════════ */
    @Transactional
    public TokenResponse refresh(String refreshToken, String deviceId) {
        log.trace("Refresh token for device={}", deviceId);
        return userService.refresh(refreshToken, deviceId);
    }

    /* ═══════════ Profil public (read-through cache) ═══════════ */
    @Transactional(readOnly = true)
    public UserResponse publicProfile(String uid) {
        return userService.getPublicProfile(uid);
    }

    /* ═══════════ Patch profil utilisateur ═══════════ */
    @Transactional
    public UserResponse updateProfile(String uid, @Valid UpdateUserRequest req) {
        return userService.updateProfile(uid, req);
    }

    /* ═══════════ Change password ═══════════ */
    @Transactional
    public void changePassword(String uid, String oldPass, String newPass) {
        userService.changePassword(uid, oldPass, newPass);
    }
}
