package com.mobility.ride.service.push;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class StubPushGateway implements PushGateway {
    @Override
    public void send(List<String> tokens, String title, String body, Map<String, String> data) {
        log.info("STUB push: title='{}' body='{}' data={} tokens={}", title, body, data, tokens);
    }
}
