package de.householdmanager.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition Root: hier werden die Beans der Anwendung verdrahtet.
 *
 * <p>Die Ports ({@code application.port.in}, {@code application.port.out})
 * werden von den Adaptern implementiert und per Konstruktorinjektion
 * aufgelöst — Spring ist damit der Wiring-Mechanismus, aber nicht die
 * Architekturentscheidung.
 */
@Configuration
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
