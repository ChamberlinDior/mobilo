package com.mobility.ride.service.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExpoPushGateway implements PushGateway {

    private final RestTemplate rest;

    @Value("${app.push.expo.api:https://exp.host/--/api/v2/push/send}")
    private String expoApi;

    @Value("${app.push.expo.maxBatch:90}")
    private int maxBatch;

    /** Payload minimal attendu par Expo */
    private record ExpoMsg(String to, String title, String body, Map<String,String> data) {}

    @Override
    public void send(List<String> tokens, String title, String body, Map<String, String> data) {
        if (tokens == null || tokens.isEmpty()) return;

        List<List<String>> batches = chunk(tokens, Math.max(1, maxBatch));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        for (List<String> batch : batches) {
            List<ExpoMsg> payload = batch.stream()
                    .map(tok -> new ExpoMsg(tok, title, body, data))
                    .collect(Collectors.toList());

            HttpEntity<List<ExpoMsg>> req = new HttpEntity<>(payload, headers);

            try {
                ResponseEntity<String> res = rest.exchange(expoApi, HttpMethod.POST, req, String.class);
                if (!res.getStatusCode().is2xxSuccessful()) {
                    log.warn("Expo push failed HTTP={} body={}", res.getStatusCode(), res.getBody());
                } else {
                    log.debug("Expo push OK ({} tokens)", batch.size());
                }
            } catch (Exception e) {
                log.warn("Expo push error: {}", e.getMessage());
            }
        }
    }

    private static <T> List<List<T>> chunk(List<T> src, int size) {
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < src.size(); i += size) {
            out.add(src.subList(i, Math.min(i + size, src.size())));
        }
        return out;
    }
}
