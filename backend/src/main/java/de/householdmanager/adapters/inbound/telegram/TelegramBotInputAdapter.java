package de.householdmanager.adapters.inbound.telegram;

import de.householdmanager.adapters.outbound.telegram.TelegramApiClient;
import de.householdmanager.adapters.outbound.telegram.TelegramApiException;
import de.householdmanager.application.port.in.TelegramChatUseCase;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.config.TelegramConfig;
import de.householdmanager.domain.model.TelegramDeliveryException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

/**
 * Eingehender Adapter: der Long-Poll des Telegram-Bots.
 *
 * <p>Die Anwendung fragt Telegram aktiv ab — kein öffentlicher Endpunkt, kein
 * Webhook, kein Zertifikat (ADR 0007, O1). Der Abfragelauf läuft im
 * Hintergrund und wird beim Herunterfahren beendet, sonst blockiert er den
 * Shutdown.
 *
 * <p>Der Adapter übersetzt nur: er nimmt Nachrichten entgegen, reicht sie an
 * den eingehenden Port weiter und schickt die Antwort des Bots zurück. Die
 * Erkennung des Freischalt-Codes und die Freischaltung selbst liegen im
 * Use Case (ADR 0005-Muster: eingehender Adapter löst genau einen Use Case
 * aus).
 *
 * <p>Ein Fehler der Abfrage beendet den Abfragelauf <strong>nicht</strong>:
 * der Lauf wartet und versucht es erneut. Ohne konfigurierten Bot-Token
 * startet der Lauf gar nicht — die Anwendung läuft trotzdem.
 */
@Slf4j
@Component
public class TelegramBotInputAdapter {

    /** Die Wartezeit der Abfrage in Sekunden — Telegram hält die Verbindung offen. */
    private static final int POLL_TIMEOUT_SECONDS = 30;

    private final TelegramApiClient api;
    private final TelegramChatUseCase telegramChatUseCase;
    private final TelegramBot telegramBot;
    private final int pollIntervalMs;
    private final boolean enabled;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile long offset;

    public TelegramBotInputAdapter(
            TelegramApiClient api,
            TelegramChatUseCase telegramChatUseCase,
            TelegramBot telegramBot,
            TelegramConfig.TelegramToken token,
            @Value("${householdmanager.telegram.poll-interval-ms:5000}") int pollIntervalMs) {
        this.api = api;
        this.telegramChatUseCase = telegramChatUseCase;
        this.telegramBot = telegramBot;
        this.pollIntervalMs = pollIntervalMs;
        this.enabled = token.isPresent();
        if (!enabled) {
            log.info("Telegram-Bot ist nicht konfiguriert — der Bot-Eingang bleibt inaktiv.");
        }
    }

    /**
     * Der Abfragelauf startet, sobald die Anwendung bereit ist. Er läuft in
     * einem eigenen Thread und endet beim Herunterfahren.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        if (!enabled || !running.compareAndSet(false, true)) {
            return;
        }
        Thread poller = new Thread(this::pollLoop, "telegram-long-poll");
        poller.setDaemon(true);
        poller.start();
        log.info("Telegram-Bot fragt Telegram ab (Long-Poll).");
    }

    /** Beendet den Abfragelauf — der Thread darf den Shutdown nicht blockieren. */
    @EventListener(org.springframework.context.event.ContextClosedEvent.class)
    public void stop() {
        running.set(false);
    }

    /**
     * Der Abfragelauf. Ein Fehler beendet ihn nicht: Telegram ist
     * gelegentlich nicht erreichbar, und der Bot soll danach weiterfragen.
     */
    void pollLoop() {
        while (running.get()) {
            pollIteration();
        }
    }

    /**
     * Eine Iteration des Abfragelaufs: eine Abfrage und die Behandlung ihres
     * Fehlers.
     *
     * <p>Eigene Methode, damit der Test den Fehlerpfad prüfen kann, ohne den
     * Lauf endlos laufen zu lassen.
     */
    void pollIteration() {
        try {
            pollOnce();
        } catch (TelegramApiException failure) {
            // Die Meldung des technischen Fehlers ist tokenfrei.
            log.warn("Die Abfrage bei Telegram ist fehlgeschlagen: {}", failure.getMessage());
            sleep();
        } catch (RestClientException failure) {
            // Sollte nicht mehr vorkommen: der API-Client kapselt jeden
            // Fehler. Die Meldung wird nicht ausgegeben — sie könnte die
            // URL mit dem Token nennen.
            log.warn("Die Abfrage bei Telegram ist fehlgeschlagen (Verbindungsfehler).");
            sleep();
        } catch (TelegramDeliveryException failure) {
            log.warn("Die Abfrage bei Telegram ist fehlgeschlagen: {}", failure.getMessage());
            sleep();
        } catch (RuntimeException failure) {
            // Auch hier wird die Meldung nicht ausgegeben: sie könnte eine
            // Anfrage-URL mit dem Token nennen. Der Typ genügt zur Diagnose.
            log.warn("Die Verarbeitung einer Telegram-Nachricht ist fehlgeschlagen ({}).",
                    failure.getClass().getSimpleName());
            sleep();
        }
    }

    /**
     * Eine Abfrage: die ausstehenden Nachrichten holen und je Nachricht den
     * Use Case aufrufen.
     */
    void pollOnce() {
        TelegramApiClient.ApiResponse<List<TelegramApiClient.ApiUpdate>> response =
                api.getUpdates(offset, POLL_TIMEOUT_SECONDS);
        if (response == null || !response.ok() || response.result() == null) {
            // Telegram hat die Abfrage abgelehnt — der Lauf versucht es erneut.
            return;
        }
        for (TelegramApiClient.ApiUpdate update : response.result()) {
            if (update.updateId() != null) {
                // Die Nachricht gilt als bestätigt, sobald sie verarbeitet ist.
                offset = update.updateId() + 1;
            }
            handle(update);
        }
    }

    /**
     * Eine Nachricht: der Use Case entscheidet, ob es eine Freischaltung ist
     * und was der Bot antwortet (AC3–AC6, AC8–AC10). Antwortet der Bot,
     * geht die Antwort zurück in den Chat.
     */
    private void handle(TelegramApiClient.ApiUpdate update) {
        if (update.message() == null || update.message().chat() == null) {
            return;
        }
        String text = update.message().text();
        if (text == null || text.isBlank()) {
            return;
        }
        String chatId = update.message().chat().id();
        if (chatId == null || chatId.isBlank()) {
            return;
        }

        TelegramChatUseCase.BotAnswer answer = telegramChatUseCase.handleMessage(chatId, text);
        if (!answer.hasMessage()) {
            // Der Bot schweigt — zum Beispiel bei einer Nachricht, die kein
            // Freischalt-Code ist (AC6).
            return;
        }
        telegramBot.sendMessage(chatId, answer.message());
    }

    /** Kurze Pause zwischen zwei Abfragen — verhindert eine dichte Schleife. */
    private void sleep() {
        try {
            Thread.sleep(pollIntervalMs);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            running.set(false);
        }
    }
}
