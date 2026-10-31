package de.householdmanager.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Baseline der Zeit: die Use Cases lesen die Zeit über eine {@code Clock}, damit der
 * Test sie einsetzen kann.
 *
 * <p>Diese Bean gehört nicht in {@link ApplicationConfig}: die Verdrahtung der Planung
 * hängt am Scheduler-Adapter, und die Use Cases hängen an dieser Bean — zusammen ergibt
 * das einen Zyklus ({@code ApplicationConfig → WasteSyncJob → Use Case → Clock}).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
