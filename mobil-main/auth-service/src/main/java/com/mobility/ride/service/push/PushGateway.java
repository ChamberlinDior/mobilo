package com.mobility.ride.service.push;

import java.util.List;
import java.util.Map;

/**
 * Abstraction d’envoi de notifications push (Expo, FCM, stub).
 * Cette interface est implémentée par des passerelles concrètes
 * (p. ex. ExpoPushGateway, FcmPushGateway, StubPushGateway) et
 * exposée via une factory sélectionnée par configuration.
 *
 * Voir application.yml -> app.push.provider : expo | fcm | stub
 */
public interface PushGateway {

    /**
     * Envoi d’un push (data/notification) à un ou plusieurs devices.
     *
     * @param tokens  Liste de device tokens (Expo: ExponentPushToken[...], FCM, etc.)
     * @param title   Titre affiché sur l’appareil
     * @param body    Corps court du message
     * @param data    Paires clé/valeur supplémentaires (type, rideId, status…)
     */
    void send(List<String> tokens, String title, String body, Map<String, String> data);
}
