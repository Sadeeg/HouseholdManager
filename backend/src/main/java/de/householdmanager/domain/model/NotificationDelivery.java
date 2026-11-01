package de.householdmanager.domain.model;

import java.time.Duration;
import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

/**
 * Fachmodell des Zustellvermerks.
 *
 * <p>Gehört zum Fachkern: keine Framework-Annotationen, keine Telegram-Typen.
 * Ein Vermerk hält fest, dass eine Nachricht an einen Chat gesendet werden
 * soll, wie oft es versucht wurde und wie der letzte Versuch endete. Der
 * Vermerk ist der persistente Zustand der Wiederholung (AC26, AC27).
 *
 * <p>Der Versuchszähler ist fachlich, nicht technisch: „höchstens drei
 * Versuche je Nachricht" (AC27) ist eine Regel der Anwendung und steht hier
 * als Konstante. Nach dem dritten Fehlversuch wird der Vermerk abgeschlossen
 * — die Verbindung des Chats bleibt bestehen (AC28).
 */
@Getter
@Builder(toBuilder = true)
public class NotificationDelivery {

    /** Höchstzahl der Versuche je Nachricht (AC27). */
    public static final int MAX_ATTEMPTS = 3;

    /**
     * Der Abstand zwischen zwei Versuchen (AC26): „die Nachricht <strong>nach
     * einer Stunde</strong> erneut versucht".
     *
     * <p>Ohne diese Grenze könnten sich die drei Versuche in Sekunden
     * statt in zwei Stunden erschöpfen — der stündliche Lauf würde jeden
     * Vermerk beim ersten Takt danach erneut versuchen. Der Vermerk, dessen
     * letzter Versuch jünger ist, wartet.
     */
    public static final Duration RETRY_INTERVAL = Duration.ofHours(1);

    /** Grenze der Spalte {@code notification_delivery.chat_id}. */
    public static final int MAX_CHAT_ID_LENGTH = 100;

    /** Grenze der Spalte {@code notification_delivery.content}. */
    public static final int MAX_CONTENT_LENGTH = 4000;

    private final Long id;
    private final long householdId;

    /** Kennung des Ziel-Chats — wer die Nachricht bekommen soll. */
    private final String chatId;

    /** Die zu versendende Nachricht. */
    private final String content;

    /** Wie oft versucht wurde — beginnt bei 0, je Fehlversuch +1. */
    private final int attemptCount;

    /** Der letzte bekannte Zustand des Versuchs. */
    private final Status status;

    /** Der Grund des letzten Fehlversuchs; null, solange es keinen gab. */
    private final String lastError;

    /** Zeitpunkt des letzten Versuchs; null, solange keiner lief. */
    private final Instant lastAttemptAt;

    /** Ob der Vermerk abgeschlossen ist — kein weiterer Versuch. */
    private final boolean completed;

    /** Der Zustand eines Zustellvermerks. */
    public enum Status {
        /** Noch nicht versucht. */
        PENDING,
        /** Der letzte Versuch war erfolgreich — der Vermerk ist erledigt. */
        DELIVERED,
        /** Der letzte Versuch ist fehlgeschlagen — ein neuer Versuch steht aus. */
        FAILED
    }

    /**
     * Der Vermerk nach einem erfolgreichen Versand: der Zähler bleibt
     * stehen, der Zustand ist {@code DELIVERED} und der Vermerk ist
     * abgeschlossen — ein erneuter Versuch ist nicht vorgesehen.
     */
    public NotificationDelivery delivered(Instant attemptAt) {
        return toBuilder()
                .attemptCount(attemptCount + 1)
                .status(Status.DELIVERED)
                .lastError(null)
                .lastAttemptAt(attemptAt)
                .completed(true)
                .build();
    }

    /**
     * Der Vermerk nach einem fehlgeschlagenen Versand: der Zähler steigt,
     * der Grund wird gehalten. Ab dem dritten Fehlversuch ist der Vermerk
     * abgeschlossen — ein vierter Versuch findet nicht statt (AC27), die
     * Verbindung bleibt aber bestehen (AC28).
     */
    public NotificationDelivery failed(Instant attemptAt, String reason) {
        int attempts = attemptCount + 1;
        return toBuilder()
                .attemptCount(attempts)
                .status(Status.FAILED)
                .lastError(reason)
                .lastAttemptAt(attemptAt)
                .completed(attempts >= MAX_ATTEMPTS)
                .build();
    }

    /** Steht ein weiterer Versuch aus? */
    public boolean isRetryable() {
        return !completed && attemptCount < MAX_ATTEMPTS;
    }

    /**
     * Steht der nächste Versuch <strong>jetzt</strong> an?
     *
     * <p>Ein Vermerk ist fällig, wenn er noch versucht werden darf
     * ({@link #isRetryable()}) und seit dem letzten Versuch mindestens
     * {@link #RETRY_INTERVAL} vergangen ist. Ein Vermerk ohne Versuch ist
     * sofort fällig.
     */
    public boolean isDueAt(Instant now) {
        if (!isRetryable()) {
            return false;
        }
        return lastAttemptAt == null || !lastAttemptAt.plus(RETRY_INTERVAL).isAfter(now);
    }
}
