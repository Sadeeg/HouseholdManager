package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import de.householdmanager.application.port.in.NotificationDispatchUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.application.port.out.NotificationDeliveryRepository;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.application.port.out.TelegramChatRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Der Use Case muss vom Container allein gebaut werden können — ohne
 * Spring-Boot-Test, mit einer minimalen Konfiguration.
 *
 * <p>Der Test deckt einen Verdacht auf: die Klasse hat zwei Konstruktoren
 * (Lombok über {@code @RequiredArgsConstructor} und einen eigenen mit der
 * Zonen-Zeichenkette). Spring wählt einen Konstruktor nur, wenn genau einer
 * existiert oder einer mit {@code @Autowired} markiert ist — sonst braucht
 * die Klasse einen Standardkonstruktor. Fehlt er, startet die Anwendung nicht.
 *
 * <p>Der Test registriert deshalb die Klasse selbst als Bean (ohne eigenen
 * {@code @Bean}-Aufruf) und prüft, ob der Container sie instanziieren kann.
 */
class NotificationDispatchUseCaseWiringTest {

    /**
     * Spring muss die Klasse selbst instanziieren können — mit genau einem
     * Konstruktor oder einem als {@code @Autowired} markierten.
     */
    @Test
    void theContainerCanInstantiateTheUseCaseOnItsOwn() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(TelegramChatRepository.class, () -> mock(TelegramChatRepository.class));
            context.registerBean(NotificationDeliveryRepository.class,
                    () -> mock(NotificationDeliveryRepository.class));
            context.registerBean(TelegramBot.class, () -> mock(TelegramBot.class));
            context.registerBean(WasteCollectionUseCase.class, () -> mock(WasteCollectionUseCase.class));
            context.registerBean(Clock.class, () -> Clock.systemUTC());
            context.registerBean(NotificationDispatchUseCaseImpl.class);

            context.refresh();

            assertThat(context.getBean(NotificationDispatchUseCase.class)).isNotNull();
        }
    }
}
