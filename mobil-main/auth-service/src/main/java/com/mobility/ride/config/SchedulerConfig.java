/* ------------------------------------------------------------------
 *  FILE : src/main/java/com/mobility/ride/config/SchedulerConfig.java
 *  v2025-09-03 – marquage @Primary pour lever le conflit d’injection
 * ------------------------------------------------------------------ */
package com.mobility.ride.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration centrale du {@link TaskScheduler} utilisé par :
 *  • {@code WaitTimeService} (grâce & frais d’attente) ;<br>
 *  • {@code CancellationPenaltyService} (frais no-show, auto-cancel) ;<br>
 *  • tout autre composant déclarant un {@code TaskScheduler} en injection.<br><br>
 *
 * Le bean est annoté {@link Primary} de façon à ce qu’il soit
 * sélectionné lorsqu’il existe d’autres schedulers dans le contexte
 * (ex. {@code messageBrokerTaskScheduler} créé par Spring WebSocket).
 */
@Configuration
@EnableScheduling               // ↳ active @Scheduled + TaskScheduler par défaut
public class SchedulerConfig {

    @Bean
    @Primary                     // ✅ résout l’ambiguïté d’injection
    public TaskScheduler taskScheduler() {

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);            // ← adapter selon la charge
        scheduler.setThreadNamePrefix("sched-");
        scheduler.initialize();
        return scheduler;                    // injecté partout sous le nom « taskScheduler »
    }
}
