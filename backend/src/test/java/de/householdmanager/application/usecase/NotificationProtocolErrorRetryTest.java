package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.application.port.out.NotificationDeliveryRepository;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.application.port.out.TelegramChatRepository;
import de.householdmanager.application.port.out.TelegramChatRepository.CodeOwner;
import de.householdmanager.domain.model.NotificationDelivery;
import de.householdmanager.domain.model.TelegramChat;
import de.householdmanager.domain.model.TelegramProtocolException;
import de.householdmanager.domain.model.WasteCollection;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Ein Protokollfehler ist kein Zustellfehler — er wird nicht wiederholt.
 *
 * <p>Die Spezifikation trennt die beiden Fehlerarten: „Telegram-Serverfehler
 * (Netz, Zeitüberschreitung, unerreichbarer Chat) sind Zustellfehler und
 * lösen den Wiederholungslauf aus; Protokollfehler (unbekannter Code,
 * ungültige Chat-ID) sind fachliche Fehler und werden protokolliert."
 *
 * <p>Ein Protokollfehler wiederholt sich aber nicht sinnvoll: Telegram lehnt
 * dieselbe Nachricht in demselben Chat wieder ab (der Bot wurde aus der
 * Gruppe entfernt, der Chat existiert nicht mehr). Deshalb darf der
 * Wiederholungslauf keinen weiteren Versuch unternehmen.
 *
 * <p>Der Test deckt genau diese Unterscheidung ab: der Versand scheitert mit
 * einem Protokollfehler, eine Stunde später läuft die Wiederholung — der Bot
 * darf <strong>nicht</strong> erneut angesprochen werden.
 */
class NotificationProtocolErrorRetryTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final long HOUSEHOLD = 1L;
    private static final String CHAT_ID = "-1001234567890";

    /** Mittwoch, 11.03.2026 — der Termin liegt am Folgetag. */
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 3, 11);

    private InMemoryChats chats;
    private InMemoryDeliveries deliveries;
    private RejectingBot bot;
    private StubWaste waste;

    @BeforeEach
    void setUp() {
        chats = new InMemoryChats();
        deliveries = new InMemoryDeliveries();
        bot = new RejectingBot();
        waste = new StubWaste();
        chats.add(HOUSEHOLD, CHAT_ID);
        waste.terms.add(WasteCollection.builder()
                .householdId(HOUSEHOLD)
                .collectionDate(WEDNESDAY.plusDays(1))
                .wasteType("Restmüll")
                .build());
    }

    /**
     * Ein Protokollfehler führt zu <em>keinem</em> weiteren Versuch.
     */
    @Test
    void aProtocolErrorIsNotRetried() {
        dispatchAt(WEDNESDAY.atTime(18, 0));

        // Der Versand ist mit einem Protokollfehler gescheitert.
        assertThat(bot.attempts).isEqualTo(1);
        assertThat(deliveries.all()).hasSize(1);

        // Eine Stunde später: der Wiederholungslauf läuft.
        retryAt(WEDNESDAY.atTime(19, 0));

        // Der Bot darf nicht erneut angesprochen worden sein.
        assertThat(bot.attempts).isEqualTo(1);
    }

    private void dispatchAt(LocalDateTime time) {
        useCase(time).dispatch();
    }

    private void retryAt(LocalDateTime time) {
        useCase(time).retryFailed();
    }

    private NotificationDispatchUseCaseImpl useCase(LocalDateTime time) {
        return new NotificationDispatchUseCaseImpl(chats, deliveries, bot, waste,
                Clock.fixed(time.atZone(ZONE).toInstant(), ZONE), ZONE.getId());
    }

    /** Die Chats als In-Memory-Fake. */
    private static final class InMemoryChats implements TelegramChatRepository {

        private final Map<Long, Map<String, TelegramChat>> chats = new LinkedHashMap<>();

        void add(long householdId, String chatId) {
            chats.computeIfAbsent(householdId, id -> new LinkedHashMap<>()).put(chatId, TelegramChat.builder()
                    .householdId(householdId)
                    .chatId(chatId)
                    .chatName("Chat " + chatId)
                    .kind(TelegramChat.ChatKind.GROUP)
                    .activatedBy("alice")
                    .build());
        }

        @Override
        public List<TelegramChat> findByHousehold(long householdId) {
            return new ArrayList<>(chats.getOrDefault(householdId, new LinkedHashMap<>()).values());
        }

        @Override
        public Optional<TelegramChat> findByHouseholdAndChatId(long householdId, String chatId) {
            return Optional.ofNullable(chats.getOrDefault(householdId, new LinkedHashMap<>()).get(chatId));
        }

        @Override
        public Optional<Long> findHouseholdByChatId(String chatId) {
            return Optional.empty();
        }

        @Override
        public List<Long> findHouseholdsWithChats() {
            return chats.keySet().stream().sorted().toList();
        }

        @Override
        public Optional<CodeOwner> findCodeOwner(String code) {
            return Optional.empty();
        }

        @Override
        public String ensureCode(long householdId, String userId) {
            return "123456";
        }

        @Override
        public TelegramChat save(TelegramChat chat) {
            return chat;
        }

        @Override
        public void delete(long householdId, String chatId) {
            // im Test nicht gebraucht
        }
    }

    /** Die Zustellvermerke als In-Memory-Fake — wie die Abfrage der Produktion. */
    private static final class InMemoryDeliveries implements NotificationDeliveryRepository {

        private final List<NotificationDelivery> deliveries = new ArrayList<>();

        @Override
        public List<NotificationDelivery> findRetryCandidates() {
            return deliveries.stream().filter(NotificationDelivery::isRetryable).toList();
        }

        @Override
        public NotificationDelivery save(NotificationDelivery delivery) {
            Long id = delivery.getId();
            deliveries.removeIf(existing -> existing.getId() != null && existing.getId().equals(id));
            NotificationDelivery stored = delivery;
            if (stored.getId() == null) {
                stored = stored.toBuilder().id((long) deliveries.size() + 1).build();
            }
            deliveries.add(stored);
            return stored;
        }

        List<NotificationDelivery> all() {
            return deliveries;
        }
    }

    /** Der Bot lehnt jede Nachricht ab — Telegram nimmt sie nicht an. */
    private static final class RejectingBot implements TelegramBot {

        private int attempts;

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public void sendMessage(String chatId, String text) {
            attempts++;
            throw new TelegramProtocolException("Telegram hat die Nachricht abgelehnt.");
        }

        @Override
        public String chatName(String chatId) {
            return "Chat";
        }

        @Override
        public ChatKind chatKind(String chatId) {
            return ChatKind.GROUP;
        }
    }

    /** Die Mülltermine als Stub — der Anzeigestand des letzten Laufs. */
    private static final class StubWaste implements WasteCollectionUseCase {

        private final List<WasteCollection> terms = new ArrayList<>();

        @Override
        public WasteOverview overview(String userId, long householdId) {
            return new WasteOverview(null, terms);
        }

        @Override
        public List<WasteCollection> collectionsOf(long householdId) {
            return terms;
        }

        @Override
        public void syncHousehold(long householdId) {
            // im Test nicht gebraucht
        }
    }
}
