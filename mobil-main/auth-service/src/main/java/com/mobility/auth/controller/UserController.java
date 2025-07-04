package com.mobility.auth.controller;

import com.mobility.auth.dto.*;
import com.mobility.auth.model.Address;
import com.mobility.auth.model.PushToken;
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

@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /* ═══════════ Profil complet ═══════════ */

    @GetMapping
    public ResponseEntity<UserResponse> getProfile(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UserResponse profile = userService.getPublicProfile(jwt.getSubject());
        return ResponseEntity.ok(profile);
    }

    /* ═══════════ PATCH profil (mobile) ═══════════ */

    @PatchMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> patchMyProfile(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid UpdateUserRequest body
    ) {
        UserResponse updated = userService.updateProfile(jwt.getSubject(), body);
        return ResponseEntity.ok(updated);
    }

    /* ═══════════ Photo de profil (BLOB) ═══════════ */

    @PostMapping(path = "/picture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadProfilePicture(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file
    ) throws IOException {
        userService.saveProfilePicture(
                jwt.getSubject(),
                file.getContentType(),
                file.getBytes()
        );
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/picture")
    public ResponseEntity<byte[]> getProfilePicture(
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProfilePicture pic = userService.getProfilePicture(jwt.getSubject());
        if (pic == null) return ResponseEntity.noContent().build();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(pic.getMimeType()))
                .cacheControl(CacheControl.noCache())
                .body(pic.getData());
    }

    /* ═══════════ Adresses enregistrées ═══════════ */

    @GetMapping("/addresses")
    public ResponseEntity<List<Address>> listAddresses(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                userService.getAddresses(jwt.getSubject())
        );
    }

    @PostMapping("/addresses")
    public ResponseEntity<Address> addAddress(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody Address address
    ) {
        Address created =
                userService.addAddress(jwt.getSubject(), address);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/addresses/{id}")
    public ResponseEntity<Void> deleteAddress(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        userService.deleteAddress(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    /* ═══════════ Contact d'urgence ═══════════ */

    @GetMapping("/emergency-contact")
    public ResponseEntity<EmergencyContact> getEmergencyContact(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                userService.getEmergencyContact(jwt.getSubject())
        );
    }

    @PutMapping("/emergency-contact")
    public ResponseEntity<EmergencyContact> updateEmergencyContact(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody EmergencyContact contact
    ) {
        EmergencyContact updated =
                userService.updateEmergencyContact(jwt.getSubject(), contact);
        return ResponseEntity.ok(updated);
    }

    /* ═══════════ Two-Factor Auth ═══════════ */

    @PutMapping("/two-factor")
    public ResponseEntity<Void> toggleTwoFactor(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam boolean enabled
    ) {
        userService.toggleTwoFactor(jwt.getSubject(), enabled);
        return ResponseEntity.noContent().build();
    }

    /* ═══════════ Push-tokens ═══════════ */

    @GetMapping("/push-tokens")
    public ResponseEntity<List<PushToken>> listPushTokens(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                userService.listPushTokens(jwt.getSubject())
        );
    }

    @PostMapping("/push-tokens")
    public ResponseEntity<PushToken> addPushToken(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody PushToken token
    ) {
        PushToken created =
                userService.addPushToken(jwt.getSubject(), token);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @DeleteMapping("/push-tokens/{tokenValue}")
    public ResponseEntity<Void> deletePushToken(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String tokenValue
    ) {
        userService.deletePushToken(jwt.getSubject(), tokenValue);
        return ResponseEntity.noContent().build();
    }

    /* ═══════════ KYC & documents ═══════════ */

    @GetMapping("/kyc/status")
    public ResponseEntity<String> getKycStatus(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                userService.getKycStatus(jwt.getSubject()).name()
        );
    }

    @PostMapping(path = "/kyc/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UrlDocument> uploadDocument(
            @AuthenticationPrincipal Jwt jwt,
            @RequestPart("file") MultipartFile file,
            @RequestParam("type") String docType
    ) throws Exception {
        UrlDocument doc =
                userService.uploadDocument(jwt.getSubject(), file, docType);
        return ResponseEntity.status(HttpStatus.CREATED).body(doc);
    }

    @GetMapping("/kyc/documents")
    public ResponseEntity<KycDocumentPage> listDocuments(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                userService.listDocuments(jwt.getSubject())
        );
    }

    /* ═══════════ Favoris & blocages ═══════════ */

    @PostMapping("/favorites/{driverId}")
    public ResponseEntity<List<Long>> addFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long driverId
    ) {
        return ResponseEntity.ok(
                userService.addFavoriteDriver(jwt.getSubject(), driverId)
        );
    }

    @DeleteMapping("/favorites/{driverId}")
    public ResponseEntity<List<Long>> removeFavorite(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long driverId
    ) {
        return ResponseEntity.ok(
                userService.removeFavoriteDriver(jwt.getSubject(), driverId)
        );
    }

    @PostMapping("/blocks/{userId}")
    public ResponseEntity<List<Long>> blockUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                userService.blockUser(jwt.getSubject(), userId)
        );
    }

    @DeleteMapping("/blocks/{userId}")
    public ResponseEntity<List<Long>> unblockUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(
                userService.unblockUser(jwt.getSubject(), userId)
        );
    }
}
