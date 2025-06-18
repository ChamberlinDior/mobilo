// src/main/java/com/mobility/auth/controller/PaymentMethodController.java
package com.mobility.auth.controller;

import com.mobility.auth.dto.*;
import com.mobility.auth.service.PaymentMethodService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CRUD des cartes bancaires de l’utilisateur connecté.
 * Toutes les routes sont protégées par JWT.
 */
@RestController
@RequestMapping("/api/v1/users/me/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService svc;

    @GetMapping
    public ResponseEntity<List<PaymentMethodResponse>> list(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(svc.list(jwt.getSubject()));
    }

    @PostMapping
    public ResponseEntity<PaymentMethodResponse> add(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid PaymentMethodRequest req
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(svc.add(jwt.getSubject(), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        svc.delete(jwt.getSubject(), id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/default")
    public ResponseEntity<PaymentMethodResponse> setDefault(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(
                svc.setDefault(jwt.getSubject(), id)
        );
    }
}
