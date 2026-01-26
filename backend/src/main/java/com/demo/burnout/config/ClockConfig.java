package com.demo.burnout.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Configuration
public class ClockConfig {
    
    @Bean
    @Profile("!demo")
    public Clock productionClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @Profile("demo")
    public Clock demoClock(@Value("${demo.clock.zone:America/New_York}") String zone,
                           @Value("${demo.clock.fixed:}") String fixed) {
        ZoneId zoneId = ZoneId.of(zone);
        
        if (fixed != null && !fixed.isBlank()) {
            return Clock.fixed(Instant.parse(fixed), zoneId);
        }
        
        // Pin to a specific Friday afternoon for predictable demo
        return Clock.fixed(
            ZonedDateTime.of(2026, 1, 30, 14, 0, 0, 0, zoneId).toInstant(),
            zoneId
        );
    }
}
