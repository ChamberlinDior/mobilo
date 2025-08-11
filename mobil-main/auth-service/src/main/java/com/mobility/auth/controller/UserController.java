// ─────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/auth/controller/UserController.java
//  v2025‑09‑20 – ajoute /me/push-tokens et /me/kyc/* (+ clean import)
// ─────────────────────────────────────────────────────────────
package com.mobility.auth.controller;

import com.mobility.auth.dto.*;
import com.mobility.auth.model.*;
import com.mobility.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /* ═══════════ Current user : /me ═══════════ */

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getPublicProfile(jwt.getSubject()));
    }

    @PatchMapping(path = "/me", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> patchMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateUserRequest body) {
        return ResponseEntity.ok(userService.updateProfile(jwt.getSubject(), body));
    }

    /* ────── profile picture (blob) ────── */

    @PostMapping(path = "/me/picture", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadProfilePicture(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file) throws IOException {

        userService.saveProfilePicture(jwt.getSubject(), file.getContentType(), file.getBytes());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/picture")
    public ResponseEntity<byte[]> getMyProfilePicture(@AuthenticationPrincipal Jwt jwt) {
        ProfilePicture pic = userService.getProfilePicture(jwt.getSubject());
        if (pic == null) return ResponseEntity.noContent().build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(pic.getMimeType()))
                .cacheControl(CacheControl.noCache())
                .body(pic.getData());
    }

    /* ═══════════ Addresses ═══════════ */

    @GetMapping("/me/addresses")
    public ResponseEntity<List<Address>> listAddresses(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getAddresses(jwt.getSubject()));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<Address> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Address address) {

        Address created = userService.addAddress(jwt.getSubject(), address);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/me/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id) {

        userService.deleteAddress(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    /* ═══════════ Emergency contact ═══════════ */

    @GetMapping("/me/emergency-contact")
    public ResponseEntity<EmergencyContact> getEmergencyContact(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getEmergencyContact(jwt.getSubject()));
    }

    @PutMapping("/me/emergency-contact")
    public ResponseEntity<EmergencyContact> updateEmergencyContact(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody EmergencyContact contact) {

        return ResponseEntity.ok(userService.updateEmergencyContact(jwt.getSubject(), contact));
    }

    /* ═══════════ 2‑FA (inchangé – placeholder) ═══════════ */
    //  /me/two-factor ON/OFF ici si besoin.

    /* ═══════════ PUSH‑TOKENS (expo / FCM) ═══════════ */

    @GetMapping("/me/push-tokens")
    public ResponseEntity<List<PushToken>> listPushTokens(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.listPushTokens(jwt.getSubject()));
    }

    @PostMapping("/me/push-tokens")
    public ResponseEntity<PushToken> addPushToken(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PushToken token) {

        PushToken saved = userService.addPushToken(jwt.getSubject(), token);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/me/push-tokens/{token}")
    public ResponseEntity<Void> deletePushToken(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String token) {

        userService.deletePushToken(jwt.getSubject(), token);
        return ResponseEntity.noContent().build();
    }

    /* ═══════════ KYC (Know‑Your‑Customer) ═══════════ */

    @GetMapping("/me/kyc/status")
    public ResponseEntity<KycStatus> getKycStatus(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.getKycStatus(jwt.getSubject()));
    }

    @GetMapping("/me/kyc/documents")
    public ResponseEntity<KycDocumentPage> listKycDocs(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(userService.listDocuments(jwt.getSubject()));
    }

    @PostMapping(path = "/me/kyc/documents", consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UrlDocument> uploadKycDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("type") String docType,
            @RequestPart("file") MultipartFile file) throws Exception {

        UrlDocument doc = userService.uploadDocument(jwt.getSubject(), file, docType);
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    /* ═══════════ NEW : photo publique d’un autre utilisateur ═══════════ */

    @GetMapping("/{id}/photo")
    public ResponseEntity<byte[]> getPhoto(@PathVariable Long id) {
        ProfilePicture pic = userService.getProfilePictureById(id);
        if (pic == null) return ResponseEntity.noContent().build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(pic.getMimeType()))
                .cacheControl(CacheControl.noCache())
                .body(pic.getData());
    }
}
