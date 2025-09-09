// ─────────────────────────────────────────────────────────────────────────────
//  FILE : src/main/java/com/mobility/ride/service/ChatService.java
//  v2025-10-11 – in-app chat + WS broadcast + notifications via NotificationService
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.service;

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

/**
 * <h2>Chat Service – in-app messaging</h2>
 *
 * ▸ Persiste chaque message.<br>
 * ▸ Diffuse le message sur WebSocket <i>/topic/ride/{id}/chat</i>.<br>
 * ▸ Déclenche une notification push aux destinataires hors-écran via NotificationService.<br>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository msgRepo;
    private final RideRepository        rideRepo;
    private final SimpMessagingTemplate simp;

    // Nouveau point d’entrée “métier” pour les push (rider/driver), route vers Expo/FCM
    private final NotificationService   notifications;

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

        /* 1) Diffusion temps-réel aux apps connectées (WebSocket/STOMP) */
        simp.convertAndSend("/topic/ride/" + rideId + "/chat", msg);

        /* 2) Notification push via service métier */
        try {
            Ride ride = rideRepo.findById(rideId).orElseThrow();

            // Destinataires = rider/driver sauf l’expéditeur
            List<Long> targets = new ArrayList<>(2);
            if (ride.getRiderId() != null && !ride.getRiderId().equals(req.senderId())) {
                targets.add(ride.getRiderId());
            }
            if (ride.getDriverId() != null && !ride.getDriverId().equals(req.senderId())) {
                targets.add(ride.getDriverId());
            }

            String preview = truncate(req.text(), 80);
            for (Long toUserId : targets) {
                notifications.notifyChat(toUserId, req.senderId(), rideId, preview);
            }
        } catch (Exception ex) {
            log.warn("⚠️  Push notification failed: {}", ex.getMessage());
        }

        log.debug("💬 [ride:{} sender:{}] {}", rideId, req.senderId(), req.text());
        return msg;
    }

    /* ═══════════ Helpers ═══════════ */
    private static String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, Math.max(0, len - 1)) + "…";
    }
}
