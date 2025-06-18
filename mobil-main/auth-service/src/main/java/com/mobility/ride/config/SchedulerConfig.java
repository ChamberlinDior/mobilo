/* ------------------------------------------------------------------
 * src/main/java/com/mobility/ride/config/SchedulerConfig.java
 * ------------------------------------------------------------------ */
package com.mobility.ride.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.TaskScheduler;

@Configuration
@EnableScheduling               // ↲ active @Scheduled & le TaskScheduler par défaut
public class SchedulerConfig {

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(4);            // nombre de threads (ajuste si besoin)
        scheduler.setThreadNamePrefix("sched-");
        scheduler.initialize();
        return scheduler;                    // sera injecté là où demandé
    }
}
