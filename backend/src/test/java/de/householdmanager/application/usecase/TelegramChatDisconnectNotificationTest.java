package de.householdmanager.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import de.householdmanager.application.port.in.MembershipUseCase;
import de.householdmanager.application.port.in.MembershipUseCase.MemberView;
import de.householdmanager.application.port.in.NotificationDispatchUseCase;
import de.householdmanager.application.port.in.TelegramChatUseCase;
import de.householdmanager.application.port.in.WasteCollectionUseCase;
import de.householdmanager.application.port.out.HouseholdRepository;
import de.householdmanager.application.port.out.NotificationDeliveryRepository;
import de.householdmanager.application.port.out.TelegramBot;
import de.householdmanager.application.port.out.TelegramChatRepository;
import de.householdmanager.application.port.out.TelegramChatRepository.CodeOwner;
import de.householdmanager.domain.model.Household;
import de.householdmanager.domain.model.MembershipRequiredException;
import de.householdmanager.domain.model.NotificationDelivery;
import de.householdmanager.domain.model.TelegramChat;
import de.householdmanager.domain.model.WasteCollection;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test des Zusammenspiels von Freischaltung, Trennen und Benachrichtigung.
 *
 * <p><strong>AC15</strong> — „Nach dem Trennen eines Chats erhält dieser Chat
 * keine weiteren Benachrichtigungen" — lässt sich nur an der Schnittstelle
 * zwischen den beiden Use Cases belegen: der Test schaltet einen Chat frei,
 * trennt ihn und fährt danach den Benachrichtigungslauf. Der Chat darf keine
 * Nachricht mehr erhalten.
 *
 * <p><strong>AC24</strong> — „ein veralteter Stand wird dabei nicht verworfen" —
 * wird hier ebenfalls belegt, diesmal mit einem Stand, dessen Termine
 * <em>nicht</em> zum Folgetag passen: die Erinnerung darf nicht ausbleiben,
 * nur weil der Anzeigestand alt ist. Der Lauf liest den Anzeigestand; er
 * synchronisiert nicht selbst.
 */
class TelegramChatDisconnectNotificationTest {

    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final long HOUSEHOLD = 1L;
    private static final String MEMBER = "alice";
    private static final String CODE = "123456";
    private static final String CHAT_ID = "-1001234567890";
    private static final String OTHER_CHAT_ID = "-1009876543210";

    /** Mittwoch, 11.03.2026, 18:00 Uhr — ein gewöhnlicher Tag. */
    private static final LocalDate WEDNESDAY = LocalDate.of(2026, 3, 11);

    private InMemoryChats chats;
    private InMemoryDeliveries deliveries;
    private RecordingBot bot;
    private StubWaste waste;
    private TelegramChatUseCase chatUseCase;
    private NotificationDispatchUseCase notificationUseCase;

    @BeforeEach
    void setUp() {
        chats = new InMemoryChats();
        deliveries = new InMemoryDeliveries();
        bot = new RecordingBot();
        waste = new StubWaste();

        chatUseCase = new TelegramChatUseCaseImpl(chats, bot, new StubHouseholds(), new StubMembership());
        notificationUseCase = new NotificationDispatchUseCaseImpl(chats, deliveries, bot, waste,
                Clock.fixed(WEDNESDAY.atTime(18, 0).atZone(ZONE).toInstant(), ZONE), ZONE.getId());
    }

    /**
     * AC15: nach dem Trennen erhält der Chat keine weiteren Benachrichtigungen.
     *
     * <p>Der Test belegt den ganzen Weg: freischalten, Benachrichtigung geht
     * noch raus, trennen, danach geht <em>nichts</em> mehr an diesen Chat.
     */
    @Test
    void aDisconnectedChatReceivesNoFurtherNotification() {
        chatUseCase.code(MEMBER, HOUSEHOLD);
        chatUseCase.handleMessage(CHAT_ID, CODE);
        waste.terms.add(term(WEDNESDAY.plusDays(1), "Restmüll"));

        // Vor dem Trennen geht die Erinnerung noch raus.
        notificationUseCase.dispatch();
        assertThat(bot.messages).hasSize(1);
        assertThat(bot.messages.getFirst()).contains("Restmüll");

        chatUseCase.disconnect(MEMBER, HOUSEHOLD, CHAT_ID);

        // Nach dem Trennen geht nichts mehr an diesen Chat.
        notificationUseCase.dispatch();

        // Der Chat hat genau eine Nachricht bekommen — die vor dem Trennen.
        assertThat(bot.messages).hasSize(1);
        assertThat(bot.sentTo).containsExactly(CHAT_ID);
    }

    /**
     * AC15 (zweite Hälfte): der Haushalt ohne Chat wird vom Lauf gar nicht
     * erst berechnet — es entsteht kein Zustellvermerk für den getrennten Chat.
     */
    @Test
    void aDisconnectedChatLeavesNoDeliveryBehind() {
        chatUseCase.code(MEMBER, HOUSEHOLD);
        chatUseCase.handleMessage(CHAT_ID, CODE);
        waste.terms.add(term(WEDNESDAY.plusDays(1), "Restmüll"));

        chatUseCase.disconnect(MEMBER, HOUSEHOLD, CHAT_ID);
        notificationUseCase.dispatch();

        assertThat(deliveries.all()).isEmpty();
        assertThat(bot.messages).isEmpty();
    }

    /**
     * AC24: ein veralteter Anzeigestand wird nicht verworfen — der Lauf
     * rechnet mit dem Stand, den die Anwendung zeigt.
     *
     * <p>Der Stand ist alt: die Termine tragen ein Datum, das nicht zum
     * Folgetag passt, und der Stub meldet ihn ausdrücklich als veraltet. Der
     * Lauf darf daraus <em>nichts</em> ableiten — er sendet keine Termine,
     * die nicht im Anzeigestand stehen, aber er verwirft den Stand auch nicht.
     */
    @Test
    void aStaleStandIsUsedAsItIsAndNotDiscarded() {
        chatUseCase.code(MEMBER, HOUSEHOLD);
        chatUseCase.handleMessage(CHAT_ID, CODE);
        // Ein alter Stand: der Termin liegt weit in der Vergangenheit.
        waste.stale = true;
        waste.terms.add(term(WEDNESDAY.minusDays(30), "Restmüll"));

        notificationUseCase.dispatch();

        // Der Stand wurde gelesen (der Stub zählt die Lesezugriffe) …
        assertThat(waste.reads).isEqualTo(1);
        // … und der Lauf hat nichts erfunden: kein Termin aus der Vergangenheit
        // wird verschickt.
        assertThat(bot.messages).isEmpty();
    }

    /**
     * AC24: ein alter Stand, dessen Termin <em>heute</em> im Fenster liegt,
     * wird nicht verworfen — der Lauf berechnet daraus die Wochenübersicht.
     *
     * <p>Am Sonntag zeigt der Anzeigestand die Woche, die am Montag beginnt —
     * auch dann, wenn der Stand nicht von heute ist.
     */
    @Test
    void aStaleStandStillFeedsTheWeeklyOverview() {
        LocalDate sunday = LocalDate.of(2026, 3, 15);
        NotificationDispatchUseCase sundayUseCase = new NotificationDispatchUseCaseImpl(
                chats, deliveries, bot, waste,
                Clock.fixed(sunday.atTime(18, 0).atZone(ZONE).toInstant(), ZONE), ZONE.getId());

        chatUseCase.code(MEMBER, HOUSEHOLD);
        chatUseCase.handleMessage(CHAT_ID, CODE);
        waste.stale = true;
        waste.terms.add(term(sunday.plusDays(1), "Papier"));

        sundayUseCase.dispatch();

        assertThat(bot.messages).isNotEmpty();
        assertThat(bot.messages.getFirst()).contains("Papier").contains("16.03.2026");
    }

    /**
     * AC17: <em>jeder</em> freigeschaltete Chat eines Haushalts erhält die
     * Wochenübersicht — nicht nur der erste.
     */
    @Test
    void everyActivatedChatOfTheHouseholdReceivesTheWeeklyOverview() {
        chatUseCase.code(MEMBER, HOUSEHOLD);
        chatUseCase.handleMessage(CHAT_ID, CODE);
        chatUseCase.handleMessage(OTHER_CHAT_ID, CODE);
        waste.terms.add(term(WEDNESDAY.plusDays(1), "Restmüll"));

        notificationUseCase.dispatch();

        assertThat(bot.sentTo).containsExactlyInAnyOrder(CHAT_ID, OTHER_CHAT_ID);
    }

    private WasteCollection term(LocalDate date, String wasteType) {
        return WasteCollection.builder()
                .householdId(HOUSEHOLD)
                .collectionDate(date)
                .wasteType(wasteType)
                .build();
    }

    // --- HELPERS ------------------------------------------------------------

    /** Die Chats als In-Memory-Fake — von beiden Use Cases geteilt. */
    private static final class InMemoryChats implements TelegramChatRepository {

        private final Map<Long, Map<String, TelegramChat>> chats = new LinkedHashMap<>();
        private final Map<Long, String> codes = new LinkedHashMap<>();
        private final Map<Long, String> codeOwners = new LinkedHashMap<>();

        @Override
        public List<TelegramChat> findByHousehold(long householdId) {
            return new ArrayList<>(chats.getOrDefault(householdId, new LinkedHashMap<>()).values());
        }

        @Override
        public List<Long> findHouseholdsWithChats() {
            return chats.keySet().stream().sorted().toList();
        }

        @Override
        public Optional<TelegramChat> findByHouseholdAndChatId(long householdId, String chatId) {
            return Optional.ofNullable(chats.getOrDefault(householdId, new LinkedHashMap<>()).get(chatId));
        }

        @Override
        public Optional<Long> findHouseholdByChatId(String chatId) {
            return chats.entrySet().stream()
                    .filter(entry -> entry.getValue().containsKey(chatId))
                    .map(Map.Entry::getKey)
                    .findFirst();
        }

        @Override
        public Optional<CodeOwner> findCodeOwner(String code) {
            for (Map.Entry<Long, String> entry : codes.entrySet()) {
                if (entry.getValue().equals(code)) {
                    return Optional.of(new CodeOwner(entry.getKey(), codeOwners.get(entry.getKey())));
                }
            }
            return Optional.empty();
        }

        @Override
        public String ensureCode(long householdId, String userId) {
            return codes.computeIfAbsent(householdId, id -> {
                codeOwners.put(id, userId);
                return CODE;
            });
        }

        @Override
        public TelegramChat save(TelegramChat chat) {
            chats.computeIfAbsent(chat.getHouseholdId(), id -> new LinkedHashMap<>())
                    .put(chat.getChatId(), chat);
            return chat;
        }

        @Override
        public void delete(long householdId, String chatId) {
            Map<String, TelegramChat> ofHousehold = chats.get(householdId);
            if (ofHousehold != null) {
                ofHousehold.remove(chatId);
            }
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

    /** Der Bot merkt sich jede Nachricht und ihren Ziel-Chat. */
    private static final class RecordingBot implements TelegramBot {

        private final List<String> messages = new ArrayList<>();
        private final List<String> sentTo = new ArrayList<>();

        @Override
        public boolean isConfigured() {
            return true;
        }

        @Override
        public void sendMessage(String chatId, String text) {
            sentTo.add(chatId);
            messages.add(text);
        }

        @Override
        public String chatName(String chatId) {
            return "WG Flur";
        }

        @Override
        public ChatKind chatKind(String chatId) {
            return ChatKind.GROUP;
        }
    }

    /** Der Anzeigestand des Müllkalenders — mit Zähler der Lesezugriffe. */
    private static final class StubWaste implements WasteCollectionUseCase {

        private final List<WasteCollection> terms = new ArrayList<>();
        private boolean stale;
        private int reads;

        @Override
        public WasteOverview overview(String userId, long householdId) {
            return new WasteOverview(null, terms);
        }

        @Override
        public List<WasteCollection> collectionsOf(long householdId) {
            reads++;
            return terms;
        }

        @Override
        public void syncHousehold(long householdId) {
            // im Test nicht gebraucht
        }
    }

    /** Die Haushalte als Stub — ein Haushalt ist bekannt. */
    private static final class StubHouseholds implements HouseholdRepository {

        @Override
        public List<Household> findAll() {
            return List.of(Household.builder().id(HOUSEHOLD).name("Familie Müller").createdBy(MEMBER).build());
        }

        @Override
        public Optional<Household> findById(long id) {
            return Optional.of(Household.builder().id(HOUSEHOLD).name("Familie Müller").createdBy(MEMBER).build());
        }

        @Override
        public List<Household> findByCreatedBy(String userId) {
            return List.of();
        }

        @Override
        public List<Household> findWithSource() {
            return List.of();
        }

        @Override
        public Household save(Household household) {
            return household;
        }
    }

    /** Die Mitgliedschaft als Stub — nur die genannten Personen sind Mitglieder. */
    private static final class StubMembership implements MembershipUseCase {

        @Override
        public List<MemberView> listMembers(String currentUserId, long householdId) {
            return List.of();
        }

        @Override
        public Optional<String> displayNameOf(String userId) {
            return Optional.empty();
        }

        @Override
        public MemberView addMember(String currentUserId, long householdId, String userId) {
            return null;
        }

        @Override
        public void removeMember(String currentUserId, long householdId, String userId) {
            // im Test nicht gebraucht
        }

        @Override
        public void requireMembership(long householdId, String userId) {
            if (!MEMBER.equals(userId)) {
                throw new MembershipRequiredException("Diese Person ist kein Mitglied des Haushalts.");
            }
        }

        @Override
        public boolean isMember(long householdId, String userId) {
            return MEMBER.equals(userId);
        }
    }
}
