package com.mobility.auth.controller;

import com.mobility.auth.dto.*;
import com.mobility.auth.service.AuthService;
import com.mobility.auth.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * <h2>REST AuthController – v1</h2>
 *
 * <p>Expose toutes les routes d’authentification / profil pour les
 * applications mobiles & front-office :</p>
 *
 * <ul>
 *   <li><strong>/signup/rider</strong> &nbsp;– création compte passager</li>
 *   <li><strong>/signup/driver</strong> – création compte chauffeur/coursier</li>
 *   <li><strong>/login</strong> &nbsp;– login + récupération JWT/refresh</li>
 *   <li><strong>/refresh</strong> – rotation refresh token ⟶ nouveau access token</li>
 *   <li><strong>/verify-email</strong> – vérification du code envoyé par mail</li>
 *   <li><strong>/users/{uid}</strong> – lecture profil public (cache)</li>
 *   <li><strong>/users/{uid}</strong> – patch profil (préférences, nom…)</li>
 *   <li><strong>/users/{uid}/password</strong> – changement mot de passe</li>
 * </ul>
 *
 * <p>
 *   • Toutes les réponses suivent la spéc RFC 9457 (Problem+JSON) en cas d’erreur.<br>
 *   • Validation Jakarta Bean Validation sur tous les DTO.<br>
 *   • Pas de logique métier : délégation à {@link AuthService} et
 *     vérification email à {@link EmailVerificationService}.
 * </p>
 */
@Tag(name = "Authentication", description = "Sign-up, login, refresh, verify-email, profile")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    /* ═══════════ Sign-up endpoints ═══════════ */

    @Operation(summary = "Sign-up rider / passager",
            responses = @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))))
    @PostMapping("/signup/rider")
    public ResponseEntity<TokenResponse> signUpRider(@RequestBody @Valid SignUpRequest body) {
        TokenResponse tokens = authService.signUpRider(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
    }

    @Operation(summary = "Sign-up driver / courier",
            responses = @ApiResponse(responseCode = "201", description = "Account created",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))))
    @PostMapping("/signup/driver")
    public ResponseEntity<TokenResponse> signUpDriver(@RequestBody @Valid SignUpRequest body) {
        TokenResponse tokens = authService.signUpDriver(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(tokens);
    }

    /* ═══════════ Login & refresh ═══════════ */

    @Operation(summary = "Login (email + password)",
            responses = @ApiResponse(responseCode = "200", description = "Authenticated",
                    content = @Content(schema = @Schema(implementation = TokenResponse.class))))
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest body) {
        return ResponseEntity.ok(authService.login(body));
    }

    @Operation(summary = "Refresh JWT (rotation)",
            responses = @ApiResponse(responseCode = "200", description = "New access/refresh pair"))
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
            @RequestParam("token") String refreshToken,
            @RequestParam(value = "deviceId", required = false) String deviceId) {
        return ResponseEntity.ok(authService.refresh(refreshToken, deviceId));
    }

    /* ═══════════ Verify email ═══════════ */

    @Operation(summary = "Vérifier le code d'email",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Email vérifié"),
                    @ApiResponse(responseCode = "400", description = "Token invalide ou expiré")
            }
    )
    @PostMapping("/verify-email")
    public ResponseEntity<Void> verifyEmail(
            @RequestParam("token") @NotBlank String token) {
        emailVerificationService.verifyToken(token);
        return ResponseEntity.noContent().build();
    }

    /* ═══════════ Public profile (cached) ═══════════ */

    @Operation(summary = "Get public profile by uid",
            responses = @ApiResponse(responseCode = "200", description = "User profile",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))))
    @GetMapping("/users/{uid}")
    public ResponseEntity<UserResponse> publicProfile(@PathVariable String uid) {
        return ResponseEntity.ok(authService.publicProfile(uid));
    }

    /* ═══════════ Update profile (partial) ═══════════ */

    @Operation(summary = "Patch profile fields",
            responses = @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))))
    @PatchMapping("/users/{uid}")
    public ResponseEntity<UserResponse> updateProfile(
            @PathVariable String uid,
            @RequestBody @Valid UpdateUserRequest body) {
        return ResponseEntity.ok(authService.updateProfile(uid, body));
    }

    /* ═══════════ Change password ═══════════ */

    @Operation(summary = "Change password",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Password changed"),
                    @ApiResponse(responseCode = "400", description = "Invalid credentials")
            }
    )
    @PutMapping("/users/{uid}/password")
    public ResponseEntity<Void> changePassword(
            @PathVariable String uid,
            @RequestBody @Valid PasswordChangeRequest body) {
        authService.changePassword(uid, body.oldPassword(), body.newPassword());
        return ResponseEntity.noContent().build();
    }
}
