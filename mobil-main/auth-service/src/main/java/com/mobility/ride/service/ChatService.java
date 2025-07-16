// ─────────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/ChatService.java
//  v2025-10-11 – push FCM aux destinataires + diffusion WS centrale
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.auth.model.PushToken;
import com.mobility.auth.repository.PushTokenRepository;
import com.mobility.ride.dto.ChatMessageRequest;
import com.mobility.ride.model.ChatMessage;
import com.mobility.ride.model.Ride;
import com.mobility.ride.repository.ChatMessageRepository;
import com.mobility.ride.repository.RideRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <h2>Chat Service – in-app messaging</h2>
 *
 * ▸ Persiste chaque message.<br>
 * ▸ Diffuse le message sur WebSocket <i>/topic/ride/{id}/chat</i>.<br>
 * ▸ Envoie une notification push aux destinataires hors-écran.<br>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository msgRepo;
    private final RideRepository        rideRepo;
    private final PushTokenRepository   pushTokenRepo;
    private final PushGateway           push;          // abstraction FCM / APNs
    private final SimpMessagingTemplate simp;

    /* ═══════════ Lecture thread ═══════════ */
    @Transactional
    public List<ChatMessage> listMessages(Long rideId) {
        return msgRepo.findAllByRideIdOrderByCreatedAtAsc(rideId);
    }

    /* ═══════════ Envoi message texte ═══════════ */
    @Transactional
    public ChatMessage sendMessage(Long rideId, ChatMessageRequest req) {

        ChatMessage msg = ChatMessage.builder()
                .rideId  (rideId)
                .senderId(req.senderId())
                .text    (req.text())
                .build();

        msgRepo.save(msg);

        /* 1) Diffusion temps-réel aux apps connectées */
        simp.convertAndSend("/topic/ride/" + rideId + "/chat", msg);

        /* 2) Notification push si destinataire hors-écran */
        try {
            Ride ride = rideRepo.findById(rideId).orElseThrow();
            List<Long> targets = new ArrayList<>(2);
            if (!ride.getRiderId().equals(req.senderId()))  targets.add(ride.getRiderId());
            if (ride.getDriverId() != null &&
                    !ride.getDriverId().equals(req.senderId())) targets.add(ride.getDriverId());

            if (!targets.isEmpty()) {
                List<PushToken> tokens = pushTokenRepo.findAllByUserIdIn(targets);
                for (PushToken t : tokens) {
                    push.send(t.getToken(),
                            "Nouveau message",
                            truncate(req.text(), 40),
                            Map.of("rideId", rideId.toString(),
                                    "senderId", req.senderId().toString()));
                }
            }
        } catch (Exception ex) {
            log.warn("⚠️  Push notification failed: {}", ex.getMessage());
        }

        log.debug("💬 [ride:{} sender:{}] {}", rideId, req.senderId(), req.text());
        return msg;
    }

    /* ═══════════ Helpers ═══════════ */
    private static String truncate(String s, int len) {
        return s.length() <= len ? s : s.substring(0, len - 1) + "…";
    }
}
