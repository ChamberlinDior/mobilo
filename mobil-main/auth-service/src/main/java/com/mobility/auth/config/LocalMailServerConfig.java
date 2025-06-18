package com.mobility.auth.config;

import org.springframework.context.annotation.*;
import org.subethamail.wiser.Wiser;

@Configuration
@Profile("local")
public class LocalMailServerConfig {

    @Bean(initMethod = "start", destroyMethod = "stop")
    public Wiser wiser() {
        // Démarre Wiser sur localhost:2500
        Wiser wiser = new Wiser(2500);
        wiser.setHostname("localhost");
        return wiser;
    }
}
