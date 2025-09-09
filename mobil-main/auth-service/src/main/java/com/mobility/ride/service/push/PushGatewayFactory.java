package com.mobility.ride.service.push;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Expose dynamiquement la passerelle active (expo | fcm | stub).
 * Pour FCM, tu ajouteras plus tard une FcmPushGateway et un case "fcm".
 */
@Component
@Primary
@RequiredArgsConstructor
public class PushGatewayFactory implements PushGateway {

    private final ExpoPushGateway expoGateway;
    private final StubPushGateway stubGateway;
    // TODO: private final FcmPushGateway fcmGateway;

    @Value("${app.push.provider:expo}")
    private String provider;

    private PushGateway active() {
        return switch (provider.toLowerCase()) {
            case "expo" -> expoGateway;
            // case "fcm"  -> fcmGateway;
            default      -> stubGateway; // “stub” (ou valeur inconnue) = no-op
        };
    }

    @Override
    public void send(java.util.List<String> tokens, String title, String body, java.util.Map<String, String> data) {
        active().send(tokens, title, body, data);
    }
}
