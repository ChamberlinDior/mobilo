package com.mobility.ride.service;

import com.mobility.auth.model.PushToken;
import com.mobility.auth.model.User;
import com.mobility.auth.repository.PushTokenRepository;
import com.mobility.auth.repository.UserRepository;
import com.mobility.ride.model.Ride;
import com.mobility.ride.model.RideStatus;
import com.mobility.ride.service.push.PushGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service métier de notifications (changement de statut ride + chat).
 * Rassemble les tokens des destinataires et délègue à PushGateway.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final PushGateway push;
    private final PushTokenRepository tokenRepo;
    private final UserRepository userRepo;

    /* ========== RIDE STATUS ========== */
    @Transactional(readOnly = true)
    public void notifyRideStatus(Ride ride) {
        if (ride == null) return;
        RideStatus s = ride.getStatus();

        String titleRider;
        String bodyRider;

        String titleDriver;
        String bodyDriver;

        switch (s) {
            case ACCEPTED -> {
                titleRider  = "Chauffeur trouvé";
                bodyRider   = "Votre course #" + ride.getId() + " a été acceptée.";
                titleDriver = "Course acceptée";
                bodyDriver  = "Vous avez accepté la course #" + ride.getId();
            }
            case EN_ROUTE -> {
                titleRider  = "Chauffeur en route";
                bodyRider   = "Il arrive vers vous. Course #" + ride.getId();
                titleDriver = "Navigation démarrée";
                bodyDriver  = "En route vers le pickup.";
            }
            case ARRIVED, WAITING -> {
                titleRider  = "Chauffeur arrivé";
                bodyRider   = "Il vous attend au point de prise en charge.";
                titleDriver = "Arrivé au pickup";
                bodyDriver  = "Patientez, puis démarrez la course.";
            }
            case IN_PROGRESS -> {
                titleRider  = "Trajet démarré";
                bodyRider   = "Bon voyage !";
                titleDriver = "Trajet démarré";
                bodyDriver  = "Conduisez prudemment.";
            }
            case COMPLETED -> {
                titleRider  = "Trajet terminé";
                bodyRider   = "Merci ! Reçu #" + ride.getId();
                titleDriver = "Course terminée";
                bodyDriver  = "Bien joué.";
            }
            case CANCELLED -> {
                titleRider  = "Course annulée";
                bodyRider   = "Votre course #" + ride.getId() + " a été annulée.";
                titleDriver = "Course annulée";
                bodyDriver  = "La course a été annulée.";
            }
            case NO_SHOW -> {
                titleRider  = "Absence constatée";
                bodyRider   = "Course #" + ride.getId() + " annulée (no-show).";
                titleDriver = "No-show";
                bodyDriver  = "No-show enregistré.";
            }
            default -> { return; }
        }

        Map<String,String> data = Map.of(
                "type", "ride_status",
                "rideId", String.valueOf(ride.getId()),
                "status", ride.getStatus().name()
        );

        // Rider
        if (ride.getRiderId() != null) {
            List<String> tks = tokensOfUser(ride.getRiderId());
            push.send(tks, titleRider, bodyRider, data);
        }

        // Driver
        if (ride.getDriverId() != null) {
            List<String> tks = tokensOfUser(ride.getDriverId());
            push.send(tks, titleDriver, bodyDriver, data);
        }
    }

    /* ========== CHAT MESSAGE ========== */
    @Transactional(readOnly = true)
    public void notifyChat(Long toUserId, Long fromUserId, Long rideId, String preview) {
        if (toUserId == null) return;

        User from = (fromUserId == null) ? null : userRepo.findById(fromUserId).orElse(null);
        String fromName = (from == null) ? "Message" :
                Optional.ofNullable(from.getFirstName()).orElse("Message");

        Map<String,String> data = new HashMap<>();
        data.put("type", "chat");
        if (rideId != null) data.put("rideId", String.valueOf(rideId));

        List<String> tks = tokensOfUser(toUserId);
        push.send(tks, fromName, (preview == null || preview.isBlank()) ? "Nouveau message" : preview, data);
    }

    private List<String> tokensOfUser(Long userId) {
        // Requiert PushTokenRepository#findAllByUserIdIn(List<Long>) — tu l’as déjà (utilisé par l’ancien ChatService)
        return tokenRepo.findAllByUserIdIn(List.of(userId))
                .stream()
                .map(PushToken::getToken)
                .collect(Collectors.toList());
    }
}
