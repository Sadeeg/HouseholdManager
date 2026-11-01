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

    /**
     * Der einzige Konstruktor — der Container muss die Klasse ohne weitere
     * Hinweise bauen können.
     *
     * <p>Die Zone kommt als Zeichenkette aus der Konfiguration und wird hier
     * in eine {@link ZoneId} übersetzt: die Anwendung rechnet in einer Zone,
     * die Telegram-API kennt keine.
     */
    public NotificationDispatchUseCaseImpl(
            TelegramChatRepository chatRepository,
            NotificationDeliveryRepository deliveryRepository,
            TelegramBot telegramBot,
            WasteCollectionUseCase wasteCollectionUseCase,
            Clock clock,
            @Value("${householdmanager.telegram.zone:Europe/Berlin}") String zone) {
        this.chatRepository = chatRepository;
        this.deliveryRepository = deliveryRepository;
        this.telegramBot = telegramBot;
        this.wasteCollectionUseCase = wasteCollectionUseCase;
        this.clock = clock;
        this.zone = ZoneId.of(zone);
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
     *
     * <p>Ein Vermerk wird erst <strong>nach einer Stunde</strong> erneut
     * versucht: zwischen zwei Versuchen liegt mindestens
     * {@link NotificationDelivery#RETRY_INTERVAL}. Ohne diese Grenze würden
     * sich die drei Versuche in Sekunden statt in zwei Stunden erschöpfen.
     */
    @Override
    @Transactional
    public void retryFailed() {
        Instant now = Instant.now(clock);
        for (NotificationDelivery delivery : deliveryRepository.findRetryCandidates()) {
            if (!delivery.isDueAt(now)) {
                // Der letzte Versuch ist noch zu frisch — der Vermerk wartet
                // bis zum nächsten stündlichen Lauf (AC26).
                continue;
            }
            if (!isChatStillConnected(delivery)) {
                // Der Chat wurde getrennt (AC15): die Nachricht erreicht ihn
                // nicht mehr — der Vermerk wird abgebrochen, nicht wiederholt.
                log.info("Vermerk für Chat {} wird abgebrochen — der Chat ist nicht mehr verbunden.",
                        delivery.getChatId());
                deliveryRepository.save(delivery.cancelled(now, "Der Ziel-Chat ist nicht mehr verbunden."));
                continue;
            }
            try {
                telegramBot.sendMessage(delivery.getChatId(), delivery.getContent());
                deliveryRepository.save(delivery.delivered(now));
            } catch (TelegramDeliveryException failure) {
                // Zustellfehler: der Vermerk zählt den Versuch, der nächste
                // Lauf versucht es erneut (AC26).
                log.warn("Zustellung an Chat {} fehlgeschlagen (Versuch {}): {}",
                        delivery.getChatId(), delivery.getAttemptCount() + 1, failure.getMessage());
                deliveryRepository.save(delivery.failed(now, failure.getMessage()));
            } catch (TelegramProtocolException failure) {
                // Protokollfehler: ein weiterer Versuch würde scheitern.
                log.warn("Telegram hat die Nachricht an Chat {} abgelehnt: {}",
                        delivery.getChatId(), failure.getMessage());
                deliveryRepository.save(delivery.rejected(now, failure.getMessage()));
            }
        }
    }

    /**
     * Ist der Ziel-Chat des Vermerks noch mit dem Haushalt verbunden?
     *
     * <p>Ein getrennter Chat darf keine Nachricht mehr bekommen (AC15) — auch
     * nicht über den Wiederholungslauf. Die Bindung wird über den
     * Haushalt des Vermerks geprüft, nicht über eine Suche nach der
     * Chat-ID allein: der Vermerk gehört zu einem Haushalt, und nur dessen
     * Chat zählt.
     */
    private boolean isChatStillConnected(NotificationDelivery delivery) {
        return chatRepository.findByHouseholdAndChatId(delivery.getHouseholdId(), delivery.getChatId())
                .isPresent();
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
     * <strong>vor</strong> dem Versand — so ist der Zustand des Versuchs
     * jederzeit nachvollziehbar.
     *
     * <p>{@code PENDING} ist ein <strong>flüchtiger Zwischenzustand</strong>:
     * das Speichern des Vermerks, der Versand und die Fortschreibung auf
     * {@code DELIVERED}, {@code FAILED} oder {@code REJECTED} passieren in
     * derselben Transaktion des Laufs. Bricht der Versand dazwischen ab,
     * wird die Transaktion verworfen — ein dauerhafter {@code PENDING}-Datensatz
     * entsteht nicht.
     *
     * <p>Der Wiederholungslauf nimmt ohnehin nur Vermerke im Zustand
     * {@code FAILED} auf — also solche, bei denen der Versuch
     * <em>nachweislich</em> fehlgeschlagen ist (AC26). Ein {@code PENDING} würde
     * auch nicht wiederholt: Telegram könnte die Nachricht schon angenommen
     * haben, ein zweiter Versand liefe auf eine doppelte Zustellung hinaus.
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
            // Protokollfehler: protokolliert, aber nicht wiederholt — dieselbe
            // Nachricht würde von Telegram wieder abgelehnt.
            log.warn("Telegram hat die Nachricht an Chat {} abgelehnt: {}", chatId, failure.getMessage());
            deliveryRepository.save(delivery.rejected(Instant.now(clock), failure.getMessage()));
        }
    }

    private String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }
}
