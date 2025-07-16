package com.mobility.ride.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Abstraction minuscule : un seul point d’accès pour
 * toutes les notifications push (FCM / APNs / WebSocket offline).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushGateway {

    // ▸ Injectez ici le SDK FCM, SNS, APNs, ou un client maison
    //   (ex. : private final FcmClient fcm;)

    /**
     * Envoie un push « data ».
     *
     * @param token      jeton FCM/APNs
     * @param title      titre affiché
     * @param body       corps court
     * @param data       paires clé/valeur supplémentaires
     */
    public void send(String token, String title, String body, Map<String, String> data) {
        try {
            // Exemple simplifié : log + TODO
            log.debug("📲 Push ➜ {} : {} – {}", token, title, body);
            // TODO : appeler le SDK réel, propager `data`
        } catch (Exception ex) {
            log.warn("⚠️  Push to {} failed : {}", token, ex.getMessage());
        }
    }
}
