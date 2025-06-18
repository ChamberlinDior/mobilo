// ─────────────────────────────────────────────────────────────────────────────
//  SERVICE : ChatService
//  Messagerie texte (conver- sations rider ↔ driver) – version repository-backed
// ─────────────────────────────────────────────────────────────────────────────
package com.mobility.ride.service;

import com.mobility.ride.dto.ChatMessageRequest;
import com.mobility.ride.model.ChatMessage;
import com.mobility.ride.repository.ChatMessageRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <h2>Chat Service – in-app messaging</h2>
 *
 * <p>
 * • Persiste chaque message dans <code>chat_messages</code> (JPA).<br>
 * • Expose deux opérations utilisées par le <em>ChatController</em> :<br>
 * &nbsp;&nbsp;• <strong>listMessages</strong> — thread complet ordonné ASC.<br>
 * &nbsp;&nbsp;• <strong>sendMessage</strong> — crée + renvoie le message.<br>
 * • Les hooks pour Twilio / Agora sont déjà en place (logs).<br>
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository msgRepo;

    /* ═══════════ Lecture thread ═══════════ */
    @Transactional
    public List<ChatMessage> listMessages(Long rideId) {
        return msgRepo.findAllByRideIdOrderByCreatedAtAsc(rideId);
    }

    /* ═══════════ Envoi message texte ═══════════ */
    @Transactional
    public ChatMessage sendMessage(Long rideId, ChatMessageRequest req) {

        ChatMessage msg = ChatMessage.builder()
                .rideId(rideId)
                .senderId(req.senderId())
                .text(req.text())
                .build();

        msgRepo.save(msg);

        // 🎯 point d’intégration (Twilio Conversations, FCM, …)
        log.debug("💬 [ride:{} sender:{}] {}", rideId, req.senderId(), req.text());

        return msg;
    }

    /* ═══════════ API bas-niveau : envoi direct (facultatif) ═══════════ */
    /**
     * Méthode utilitaire conservée pour un appel direct hors REST (ex : web-socket).
     */
    public void sendDirectMessage(long fromUserId, long toUserId, String body) {
        log.debug("💬 [direct {} ➜ {}] {}", fromUserId, toUserId, body);
        // TODO : appeler Twilio Conversations / push, si nécessaire
    }

    /* ═══════════ VoIP anonymisé (stub) ═══════════ */
    public String createVoiceSession(long userA, long userB) {
        String callId = "call-" + System.nanoTime();
        log.info("📞 Voice session {} between {} and {}", callId, userA, userB);
        // TODO : générer token (Agora.io / Twilio Voice)
        return callId;
    }
}
