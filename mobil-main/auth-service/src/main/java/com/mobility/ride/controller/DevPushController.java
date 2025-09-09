// ─────────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/controller/DevPushController.java
//  Profil: local uniquement – endpoints de test push
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.controller;

import com.mobility.auth.model.PushToken;
import com.mobility.auth.repository.PushTokenRepository;
import com.mobility.ride.service.push.PushGateway;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Slf4j
@Profile("local") // actif seulement en profil local
@RestController
@RequestMapping("/api/dev/push")
@RequiredArgsConstructor
public class DevPushController {

    private final PushGateway push;
    private final PushTokenRepository tokens;

    // DTO simple pour POST /token
    public record TestPushRequest(
            @NotBlank String token,
            String title,
            String body,
            Map<String,String> data
    ) {}

    /**
     * Envoie un push de test à un token Expo/FCM fourni.
     * Ex: POST /api/dev/push/token
     * Body: { "token": "ExponentPushToken[...]" }
     */
    @PostMapping("/token")
    public ResponseEntity<Map<String,Object>> testByToken(@RequestBody TestPushRequest req) {
        String title = Optional.ofNullable(req.title()).orElse("🚀 Backend OK");
        String body  = Optional.ofNullable(req.body()).orElse("Test push (profil local)");
        Map<String,String> data = Optional.ofNullable(req.data()).orElse(Map.of("type", "smoke"));

        push.send(List.of(req.token()), title, body, data);

        Map<String,Object> out = new HashMap<>();
        out.put("sent", 1);
        out.put("token", req.token());
        return ResponseEntity.ok(out);
    }

    /**
     * Envoie un push de test à TOUS les tokens d’un utilisateur.
     * Ex: POST /api/dev/push/user/123
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<Map<String,Object>> testByUser(@PathVariable Long userId) {
        List<PushToken> list = tokens.findAllByUserIdIn(List.of(userId));
        List<String> to = list.stream().map(PushToken::getToken).toList();

        if (to.isEmpty()) {
            return ResponseEntity.ok(Map.of("sent", 0, "userId", userId, "message", "aucun token"));
        }

        push.send(to, "🚀 Backend OK", "Test push (profil local)", Map.of("type", "smoke"));
        return ResponseEntity.ok(Map.of("sent", to.size(), "userId", userId, "tokens", to));
    }
}
