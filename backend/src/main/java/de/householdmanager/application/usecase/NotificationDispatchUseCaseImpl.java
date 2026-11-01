package de.householdmanager.application.usecase;

import de.householdmanager.application.port.in.NotificationDispatchUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.application.port.out.NotificationDeliveryRepository;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.application.port.out.TelegramChatRepository;
import de.householdmanager.domain.model.NotificationDelivery;
import de.householdmanager.domain.model.TelegramChat;
import de.householdmanager.domain.model.TelegramDeliveryException;
import de.householdmanager.domain.model.TelegramProtocolException;
import de.householdmanager.domain.model.WasteCollection;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use Case „Benachrichtigung" — Datumsberechnung, Nachrichtenaufbau und
 * Zustellvermerk.
 *
 * <p>Die Zeitregeln liegen hier, nicht im Adapter: „morgen" und „kommende
 * Woche" sind Domänenregeln und stehen hier mit der injizierten
 * {@link Clock} — die Telegram-API kennt sie nicht (ADR 0007).
 *
 * <p>Die Termine kommen über den In-Port des Müllkalender-Kontexts
 * ({@link WasteCollectionUseCase}) — nicht über dessen Tabellen. Der
 * Anzeigestand wird nicht verworfen, wenn er veraltet ist: ein veralteter
 * Stand ist die Aussage der Anwendung (AC24).
 *
 * <p>Fehlerisolierung auf zwei Ebenen: ein fehlgeschlagener **Chat** stoppt
 * die übrigen Chats desselben Haushalts nicht (AC29), ein fehlgeschlagener
 * **Haushalt** stoppt die übrigen Haushalte nicht.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationDispatchUseCaseImpl implements NotificationDispatchUseCase {

    /** Das Datum in der Nachricht: „12.03.2026" (dt. Format). */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    /** Der Wochentag in der Nachricht: „Donnerstag" — immer auf Deutsch. */
    private static final DateTimeFormatter WEEKDAY_FORMAT =
            DateTimeFormatter.ofPattern("EEEE", Locale.GERMAN);

    private final TelegramChatRepository chatRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final TelegramBot telegramBot;
    private final WasteCollectionUseCase wasteCollectionUseCase;
    private final Clock clock;
    private final ZoneId zone;

    public NotificationDispatchUseCaseImpl(
            TelegramChatRepository chatRepository,
            NotificationDeliveryRepository deliveryRepository,
            TelegramBot telegramBot,
            WasteCollectionUseCase wasteCollectionUseCase,
            Clock clock,
            @Value("${householdmanager.telegram.zone:Europe/Berlin}") String zone) {
        this(chatRepository, deliveryRepository, telegramBot, wasteCollectionUseCase,
                clock, ZoneId.of(zone));
    }

    // --- Der 18:00-Uhr-Lauf --------------------------------------------------

    /**
     * Der Lauf: die Wochenübersicht und die Erinnerung für den Folgetag.
     *
     * <p>Am Sonntag kommen <strong>beide</strong> Nachrichten: die
     * Wochenübersicht der Woche, die am Montag beginnt, und die Erinnerung
     * für den Montag (AC20). An jedem anderen Tag kommt nur die Erinnerung —
     * und nur, wenn der Folgetag einen Termin hat (AC23).
     *
     * <p>Ein Haushalt ohne freigeschalteten Chat erhält nichts (AC25) — ohne
     * Chat wird nichts berechnet und nichts versendet.
     */
    @Override
    @Transactional
    public void dispatch() {
        LocalDate today = LocalDate.now(clock.withZone(zone));

        for (long householdId : chatRepository.findHouseholdsWithChats()) {
            try {
                dispatchFor(householdId, today);
            } catch (RuntimeException failure) {
                // Ein fehlerhafter Haushalt stoppt die anderen nicht.
                log.warn("Benachrichtigung für Haushalt {} fehlgeschlagen: {}",
                        householdId, failure.getMessage());
            }
        }
    }

    /**
     * Die Nachrichten eines Haushalts: Wochenübersicht (sonntags) und
     * Erinnerung für den Folgetag.
     */
    private void dispatchFor(long householdId, LocalDate today) {
        List<TelegramChat> chats = chatRepository.findByHousehold(householdId);
        if (chats.isEmpty()) {
            // Ohne freigeschalteten Chat wird nichts berechnet (AC25).
            return;
        }

        List<WasteCollection> collections = wasteCollectionUseCase.collectionsOf(householdId);

        List<String> messages = new java.util.ArrayList<>();
        if (today.getDayOfWeek() == DayOfWeek.SUNDAY) {
            // Sonntags: die Wochenübersicht der kommenden Woche (AC20).
            messages.add(weeklyOverview(collections, today));
        }

        // Die Erinnerung für den Folgetag — an jedem Tag, auch am Sonntag (AC20, AC21).
        LocalDate tomorrow = today.plusDays(1);
        List<WasteCollection> tomorrowTerms = collections.stream()
                .filter(collection -> collection.getCollectionDate().equals(tomorrow))
                .sorted(Comparator.comparing(WasteCollection::getWasteType))
                .toList();
        if (!tomorrowTerms.isEmpty()) {
            messages.add(reminder(tomorrow, tomorrowTerms));
        }

        for (String message : messages) {
            for (TelegramChat chat : chats) {
                // Ein fehlgeschlagener Chat stoppt die übrigen nicht (AC29).
                send(householdId, chat.getChatId(), message);
            }
        }
    }

    // --- Der stündliche Wiederholungslauf ------------------------------------

    /**
     * Die Wiederholung: alle Vermerke, die noch einen Versuch brauchen, werden
     * erneut versucht (AC26). Nach dem dritten Fehlversuch wird kein weiterer
     * Versuch unternommen (AC27); die Verbindung des Chats bleibt bestehen
     * (AC28).
     */
    @Override
    @Transactional
    public void retryFailed() {
        for (NotificationDelivery delivery : deliveryRepository.findRetryable()) {
            try {
                telegramBot.sendMessage(delivery.getChatId(), delivery.getContent());
                deliveryRepository.save(delivery.delivered(Instant.now(clock)));
            } catch (TelegramDeliveryException failure) {
                // Zustellfehler: der Vermerk zählt den Versuch, der nächste
                // Lauf versucht es erneut (AC26).
                log.warn("Zustellung an Chat {} fehlgeschlagen (Versuch {}): {}",
                        delivery.getChatId(), delivery.getAttemptCount() + 1, failure.getMessage());
                deliveryRepository.save(delivery.failed(Instant.now(clock), failure.getMessage()));
            } catch (TelegramProtocolException failure) {
                // Protokollfehler: ein weiterer Versuch würde scheitern.
                log.warn("Telegram hat die Nachricht an Chat {} abgelehnt: {}",
                        delivery.getChatId(), failure.getMessage());
                deliveryRepository.save(delivery.failed(Instant.now(clock), failure.getMessage()));
            }
        }
    }

    // --- Nachrichtenaufbau ---------------------------------------------------

    /**
     * Die Wochenübersicht der kommenden Woche: alle Termine, je Zeile Datum
     * und Abfalltyp, aufsteigend nach Datum (AC17, AC18). Ist die Woche leer,
     * sagt die Nachricht das ausdrücklich (AC19).
     */
    private String weeklyOverview(List<WasteCollection> collections, LocalDate today) {
        LocalDate monday = today.with(DayOfWeek.MONDAY).plusWeeks(1);
        LocalDate sunday = monday.plusDays(6);

        List<WasteCollection> week = collections.stream()
                .filter(collection -> {
                    LocalDate date = collection.getCollectionDate();
                    return !date.isBefore(monday) && !date.isAfter(sunday);
                })
                .sorted(Comparator.comparing(WasteCollection::getCollectionDate)
                        .thenComparing(WasteCollection::getWasteType))
                .toList();

        if (week.isEmpty()) {
            return "Mülltermine der kommenden Woche (" + formatDate(monday) + " bis " + formatDate(sunday)
                    + "):\n\nKeine Termine in dieser Woche.";
        }

        StringBuilder text = new StringBuilder("Mülltermine der kommenden Woche (")
                .append(formatDate(monday)).append(" bis ").append(formatDate(sunday)).append("):");
        for (WasteCollection collection : week) {
            text.append('\n').append("• ").append(formatDate(collection.getCollectionDate()))
                    .append(" — ").append(collection.getWasteType());
        }
        return text.toString();
    }

    /**
     * Die Erinnerung für den Folgetag: alle Abfalltypen des Tages in
     * <strong>einer</strong> Nachricht, je Zeile Wochentag, Datum und
     * Abfalltyp, sortiert nach Abfalltyp (AC21, AC22).
     */
    private String reminder(LocalDate date, List<WasteCollection> terms) {
        StringBuilder text = new StringBuilder("Müllabfuhr morgen — ")
                .append(date.format(WEEKDAY_FORMAT)).append(", ").append(formatDate(date)).append(':');
        for (WasteCollection collection : terms) {
            text.append('\n').append("• ").append(collection.getWasteType());
        }
        return text.toString();
    }

    // --- Zustellung ----------------------------------------------------------

    /**
     * Der Versand einer Nachricht an einen Chat. Der Vermerk entsteht
     * <strong>vor</strong> dem Versand — so geht bei einem Absturz zwischen
     * Versand und Vermerk nichts verloren: der Vermerk bleibt und der
     * Wiederholungslauf versucht es erneut (AC26).
     */
    private void send(long householdId, String chatId, String message) {
        NotificationDelivery delivery = deliveryRepository.save(NotificationDelivery.builder()
                .householdId(householdId)
                .chatId(chatId)
                .content(message)
                .attemptCount(0)
                .status(NotificationDelivery.Status.PENDING)
                .completed(false)
                .build());

        try {
            telegramBot.sendMessage(chatId, message);
            deliveryRepository.save(delivery.delivered(Instant.now(clock)));
        } catch (TelegramDeliveryException failure) {
            // Zustellfehler: der Fehler wird protokolliert, der Vermerk zählt
            // den Versuch (AC26).
            log.warn("Zustellung an Chat {} fehlgeschlagen: {}", chatId, failure.getMessage());
            deliveryRepository.save(delivery.failed(Instant.now(clock), failure.getMessage()));
        } catch (TelegramProtocolException failure) {
            // Protokollfehler: protokolliert, aber nicht wiederholt.
            log.warn("Telegram hat die Nachricht an Chat {} abgelehnt: {}", chatId, failure.getMessage());
            deliveryRepository.save(delivery.failed(Instant.now(clock), failure.getMessage()));
        }
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
}
