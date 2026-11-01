package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.application.port.out.NotificationDeliveryRepository;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.application.port.out.TelegramChatRepository;
import de.householdmanager.application.port.out.TelegramChatRepository.CodeOwner;
import de.householdmanager.domain.model.NotificationDelivery;
import de.householdmanager.domain.model.TelegramChat;
import de.householdmanager.domain.model.TelegramDeliveryException;
import de.householdmanager.domain.model.WasteCollection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test des Benachrichtigungslaufs gegen die ausgehenden Ports — ohne
 * Spring, ohne Datenbank, ohne Telegram.
 *
 * <p>Belegt AC17–AC27 und AC29: die Wochenübersicht, die Erinnerung, der
 * leere Sonntag, die leere Woche, der Fehlerfall, die drei Versuche und die
 * Fehlerisolierung je Chat. Die Zeit ist eine feste {@code Clock} — der
 * Wochentag entscheidet die Nachrichtenart (Q17).
 */
class NotificationDispatchUseCaseImplTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final long HOUSEHOLD = 1L;
    private static final String CHAT_ONE = "chat-1";
    private static final String CHAT_TWO = "chat-2";

    /** Mittwoch, 11.03.2026, 18:00 Uhr — ein gewöhnlicher Tag. */
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 3, 11);

    /** Sonntag, 15.03.2026, 18:00 Uhr — der Tag der Wochenübersicht. */
    private static final LocalDate SUNDAY = LocalDate.of(2026, 3, 15);

    private InMemoryChats chats;
    private InMemoryDeliveries deliveries;
    private StubBot bot;
    private StubWaste waste;

    @BeforeEach
    void setUp() {
        chats = new InMemoryChats();
        deliveries = new InMemoryDeliveries();
        bot = new StubBot();
        waste = new StubWaste();
    }

    /** Der Use Case mit einer festen Zeit — der Wochentag entscheidet die Nachrichtenart. */
    private NotificationDispatchUseCaseImpl useCaseOn(LocalDate day) {
        Instant now = day.atTime(18, 0).atZone(ZONE).toInstant();
        return new NotificationDispatchUseCaseImpl(chats, deliveries, bot, waste,
                Clock.fixed(now, ZONE), ZONE.getId());
    }

    /** Der Use Case mit einer festen Zeit — für Tests, die den Abstand zählen. */
    private NotificationDispatchUseCaseImpl useCaseAt(LocalDate day, LocalTime time) {
        Instant now = day.atTime(time).atZone(ZONE).toInstant();
        return new NotificationDispatchUseCaseImpl(chats, deliveries, bot, waste,
                Clock.fixed(now, ZONE), ZONE.getId());
    }

    // --- AC17, AC18: die Wochenübersicht ------------------------------------

    /** AC17, AC18: die Wochenübersicht nennt alle Termine der kommenden Woche. */
    @Test
    void dispatch_onSunday_sendsTheWeeklyOverviewWithEveryTermOfTheComingWeek() {
        givenChats(CHAT_ONE);
        givenTerm(SUNDAY.plusDays(4), "Papier"); // Donnerstag der kommenden Woche
        givenTerm(SUNDAY.plusDays(2), "Restmüll"); // Dienstag der kommenden Woche

        useCaseOn(SUNDAY).dispatch();

        assertThat(bot.messages).isNotEmpty();
        String sent = bot.messages.getFirst();
        assertThat(sent).contains("17.03.2026").contains("Restmüll");
        assertThat(sent).contains("19.03.2026").contains("Papier");
    }

    /** AC17: die Termine stehen aufsteigend nach Datum. */
    @Test
    void dispatch_sortsTheWeeklyOverviewByDate() {
        givenChats(CHAT_ONE);
        givenTerm(SUNDAY.plusDays(4), "Papier");
        givenTerm(SUNDAY.plusDays(2), "Restmüll");

        useCaseOn(SUNDAY).dispatch();

        String sent = bot.messages.getFirst();
        assertThat(sent.indexOf("Restmüll")).isLessThan(sent.indexOf("Papier"));
    }

    /** AC17: ein Termin der laufenden Woche steht nicht in der kommenden Woche. */
    @Test
    void dispatch_leavesOutTheTermsOfTheCurrentWeek() {
        givenChats(CHAT_ONE);
        givenTerm(SUNDAY.minusDays(2), "Bio"); // Freitag der laufenden Woche
        givenTerm(SUNDAY.plusDays(2), "Restmüll");

        useCaseOn(SUNDAY).dispatch();

        assertThat(bot.messages.getFirst()).doesNotContain("Bio");
    }

    /** AC19: eine leere Woche wird ausdrücklich genannt. */
    @Test
    void dispatch_forAnEmptyWeek_saysSo() {
        givenChats(CHAT_ONE);
        givenTerm(SUNDAY, "Restmüll"); // heute — nicht in der kommenden Woche

        useCaseOn(SUNDAY).dispatch();

        assertThat(bot.messages).isNotEmpty();
        assertThat(bot.messages.getFirst()).contains("Keine Termine");
    }

    /** AC18: die Wochenübersicht nennt je Termin den Abfalltyp und das Datum. */
    @Test
    void dispatch_namesWasteTypeAndDateForEveryTerm() {
        givenChats(CHAT_ONE);
        givenTerm(SUNDAY.plusDays(2), "Restmüll");

        useCaseOn(SUNDAY).dispatch();

        assertThat(bot.messages.getFirst()).contains("Restmüll").contains("17.03.2026");
    }

    // --- AC20: der Sonntag --------------------------------------------------

    /** AC20: am Sonntag kommen die Wochenübersicht und die Erinnerung für den Montag. */
    @Test
    void dispatch_onSunday_sendsTheWeeklyOverviewAndTheMondayReminder() {
        givenChats(CHAT_ONE);
        givenTerm(SUNDAY.plusDays(1), "Restmüll"); // Montag

        useCaseOn(SUNDAY).dispatch();

        assertThat(bot.messages).hasSize(2);
        assertThat(bot.messages.get(0)).contains("kommenden Woche");
        assertThat(bot.messages.get(1)).contains("Müllabfuhr morgen").contains("Montag");
    }

    /** AC20: am Sonntag beginnt die kommende Woche am Montag. */
    @Test
    void dispatch_onSunday_theWeeklyOverviewStartsOnMonday() {
        givenChats(CHAT_ONE);
        givenTerm(SUNDAY.plusDays(1), "Restmüll"); // Montag

        useCaseOn(SUNDAY).dispatch();

        assertThat(bot.messages.getFirst()).contains("16.03.2026").contains("22.03.2026");
    }

    // --- AC21, AC22, AC23: die Erinnerung -----------------------------------

    /** AC21: die Erinnerung nennt den Abfalltyp und das Datum des Termins. */
    @Test
    void dispatch_sendsAReminderForTheNextDay() {
        givenChats(CHAT_ONE);
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");

        useCaseOn(WEDNESDAY).dispatch();

        assertThat(bot.messages).hasSize(1);
        assertThat(bot.messages.getFirst())
                .contains("Restmüll")
                .contains("12.03.2026")
                .contains("Donnerstag");
    }

    /** AC22: alle Abfalltypen des Folgetags stehen in einer Nachricht. */
    @Test
    void dispatch_namesEveryWasteTypeOfTheNextDayInOneMessage() {
        givenChats(CHAT_ONE);
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");
        givenTerm(WEDNESDAY.plusDays(1), "Papier");

        useCaseOn(WEDNESDAY).dispatch();

        assertThat(bot.messages).hasSize(1);
        String sent = bot.messages.getFirst();
        assertThat(sent).contains("Restmüll").contains("Papier");
    }

    /** AC22: die Abfalltypen eines Tages stehen sortiert nach Abfalltyp. */
    @Test
    void dispatch_sortsTheReminderByWasteType() {
        givenChats(CHAT_ONE);
        givenTerm(WEDNESDAY.plusDays(1), "Papier");
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");

        useCaseOn(WEDNESDAY).dispatch();

        String sent = bot.messages.getFirst();
        assertThat(sent.indexOf("Papier")).isLessThan(sent.indexOf("Restmüll"));
    }

    /** AC23: ohne Termin am Folgetag wird keine Erinnerung gesendet. */
    @Test
    void dispatch_withoutATermTheNextDay_sendsNoReminder() {
        givenChats(CHAT_ONE);
        givenTerm(WEDNESDAY.plusDays(3), "Restmüll"); // erst übermorgen

        useCaseOn(WEDNESDAY).dispatch();

        assertThat(bot.messages).isEmpty();
    }

    /** AC23: an einem Tag ohne Termin am Folgetag geht gar nichts raus. */
    @Test
    void dispatch_withoutAnyTerm_sendsNothing() {
        givenChats(CHAT_ONE);

        useCaseOn(WEDNESDAY).dispatch();

        assertThat(bot.messages).isEmpty();
    }

    // --- AC24, AC25: der Stand und der Haushalt ohne Chat --------------------

    /** AC24: die Termine kommen aus dem Anzeigestand — ein veralteter Stand wird nicht verworfen. */
    @Test
    void dispatch_usesTheDisplayedTermsEvenIfTheStandIsStale() {
        givenChats(CHAT_ONE);
        waste.stale = true;
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");

        useCaseOn(WEDNESDAY).dispatch();

        assertThat(bot.messages).hasSize(1);
        assertThat(bot.messages.getFirst()).contains("Restmüll");
    }

    /** AC25: ein Haushalt ohne freigeschalteten Chat erhält keine Benachrichtigung. */
    @Test
    void dispatch_withoutAChat_sendsNothing() {
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");

        useCaseOn(WEDNESDAY).dispatch();

        assertThat(bot.messages).isEmpty();
    }

    // --- AC26, AC27, AC28, AC29: die Zustellung ----------------------------

    /** AC26: ein fehlgeschlagener Versand wird protokolliert und erneut versucht. */
    @Test
    void retryFailed_retryAfterAFailedDelivery() {
        givenChats(CHAT_ONE);
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");
        bot.failure = new TelegramDeliveryException("Telegram ist nicht erreichbar.");

        useCaseOn(WEDNESDAY).dispatch();

        assertThat(bot.messages).isEmpty();
        assertThat(deliveries.findRetryCandidates()).hasSize(1);
        assertThat(deliveries.findRetryCandidates().getFirst().getAttemptCount()).isEqualTo(1);

        // Der Wiederholungslauf eine Stunde später.
        bot.failure = null;
        useCaseAt(WEDNESDAY, LocalTime.of(19, 0)).retryFailed();

        assertThat(bot.messages).hasSize(1);
        assertThat(deliveries.findRetryCandidates()).isEmpty();
    }

    /** AC27: nach drei Fehlversuchen wird kein weiterer Versuch unternommen. */
    @Test
    void retryFailed_afterThreeFailures_doesNotTryAgain() {
        givenChats(CHAT_ONE);
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");
        bot.failure = new TelegramDeliveryException("Telegram ist nicht erreichbar.");

        useCaseOn(WEDNESDAY).dispatch();
        useCaseAt(WEDNESDAY, LocalTime.of(19, 0)).retryFailed();
        useCaseAt(WEDNESDAY, LocalTime.of(20, 0)).retryFailed();

        assertThat(bot.messages).isEmpty();
        // Der dritte Fehlversuch schließt den Vermerk ab.
        assertThat(deliveries.findRetryCandidates()).isEmpty();
        assertThat(deliveries.all()).hasSize(1);
        assertThat(deliveries.all().getFirst().getAttemptCount()).isEqualTo(3);
        assertThat(deliveries.all().getFirst().isCompleted()).isTrue();
    }

    /** AC28: die Verbindung des Chats bleibt nach den Fehlversuchen bestehen. */
    @Test
    void retryFailed_afterThreeFailures_keepsTheConnection() {
        givenChats(CHAT_ONE);
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");
        bot.failure = new TelegramDeliveryException("Telegram ist nicht erreichbar.");

        useCaseOn(WEDNESDAY).dispatch();
        useCaseAt(WEDNESDAY, LocalTime.of(19, 0)).retryFailed();
        useCaseAt(WEDNESDAY, LocalTime.of(20, 0)).retryFailed();

        assertThat(chats.findByHousehold(HOUSEHOLD)).hasSize(1);
    }

    /**
     * AC26: „die Nachricht <strong>nach einer Stunde</strong> erneut
     * versucht".
     *
     * <p>Ein Vermerk, dessen letzter Versuch erst wenige Minuten
     * zurückliegt, darf <strong>noch nicht</strong> wiederholt werden. Ohne
     * diese Grenze erschöpften sich die drei Versuche in Sekunden statt in
     * zwei Stunden.
     */
    @Test
    void retryFailed_waitsAnHourBeforeTheNextAttempt() {
        givenChats(CHAT_ONE);
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");
        bot.failure = new TelegramDeliveryException("Telegram ist nicht erreichbar.");

        NotificationDispatchUseCaseImpl useCase = useCaseOn(WEDNESDAY);
        useCase.dispatch();

        // Der Lauf eine halbe Stunde später: der Vermerk ist noch nicht fällig.
        useCaseAt(WEDNESDAY, LocalTime.of(18, 30)).retryFailed();

        assertThat(bot.messages).isEmpty();
        assertThat(deliveries.findRetryCandidates().getFirst().getAttemptCount()).isEqualTo(1);
    }

    /** AC26: nach einer Stunde wird der Vermerk erneut versucht. */
    @Test
    void retryFailed_retriesAfterAnHour() {
        givenChats(CHAT_ONE);
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");
        bot.failure = new TelegramDeliveryException("Telegram ist nicht erreichbar.");

        useCaseOn(WEDNESDAY).dispatch();
        bot.failure = null;

        useCaseAt(WEDNESDAY, LocalTime.of(19, 0)).retryFailed();

        assertThat(bot.messages).hasSize(1);
    }

    /** AC29: ein fehlgeschlagener Chat stoppt die übrigen Chats nicht. */
    @Test
    void dispatch_aFailedChatDoesNotStopTheOtherChats() {
        givenChats(CHAT_ONE);
        givenChats(CHAT_TWO);
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");
        bot.failingChatId = CHAT_ONE;

        useCaseOn(WEDNESDAY).dispatch();

        assertThat(bot.messages).hasSize(1);
        assertThat(bot.messages.getFirst()).contains("Restmüll");
        // Der Vermerk des fehlgeschlagenen Chats zählt den Fehlversuch.
        assertThat(deliveries.all()).hasSize(2);
    }

    /** Ein fehlgeschlagener Haushalt stoppt die übrigen Haushalte nicht. */
    @Test
    void dispatch_aFailedHouseholdDoesNotStopTheOtherHouseholds() {
        givenChats(CHAT_ONE);
        chats.failOnHousehold = HOUSEHOLD;
        givenTerm(WEDNESDAY.plusDays(1), "Restmüll");

        // Der Lauf endet ohne Fehler — der Haushalt ist isoliert.
        useCaseOn(WEDNESDAY).dispatch();

        assertThat(bot.messages).isEmpty();
    }

    // --- HELPERS ------------------------------------------------------------

    private void givenChats(String chatId) {
        chats.add(HOUSEHOLD, chatId);
    }

    private void givenTerm(LocalDate date, String wasteType) {
        waste.terms.add(WasteCollection.builder()
                .householdId(HOUSEHOLD)
                .collectionDate(date)
                .wasteType(wasteType)
                .build());
    }

    /** Die Chats als In-Memory-Fake. */
    private static final class InMemoryChats implements TelegramChatRepository {

        private final Map<Long, Map<String, TelegramChat>> chats = new LinkedHashMap<>();
        private Long failOnHousehold;

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
            if (Long.valueOf(householdId).equals(failOnHousehold)) {
                throw new IllegalStateException("Der Haushalt ist nicht erreichbar.");
            }
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

    /** Die Zustellvermerke als In-Memory-Fake. */
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

    /** Der Bot als Stub — die gesendeten Nachrichten werden gesammelt. */
    private static final class StubBot implements TelegramBot {

        private final List<String> messages = new ArrayList<>();
        private RuntimeException failure;
        private String failingChatId;

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public void sendMessage(String chatId, String text) {
            if (failingChatId != null && failingChatId.equals(chatId)) {
                throw failure != null ? failure : new TelegramDeliveryException("nicht erreichbar");
            }
            if (failure != null) {
                throw failure;
            }
            messages.add(text);
        }

        @Override
        public String chatName(String chatId) {
            return "Chat";
        }

        @Override
        public ChatKind chatKind(String chatId) {
            return ChatKind.PRIVATE;
        }
    }

    /** Die Mülltermine als Stub — der Anzeigestand des letzten Laufs. */
    private static final class StubWaste implements WasteCollectionUseCase {

        private final List<WasteCollection> terms = new ArrayList<>();
        private boolean stale;

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
